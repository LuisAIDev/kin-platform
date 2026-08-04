import { useEffect, useRef, useState } from "react";
import { API_URL, authHeaders } from "@/services/enterpriseApi";
import {
  TERMINAL_STATES,
  type EnterpriseProgressEvent,
} from "@/types/enterprise";

/** Estado de la conexión SSE expuesto por el hook. */
export interface ProgressConnection {
  events: EnterpriseProgressEvent[];
  connected: boolean;
  error: string | null;
  terminal: boolean;
}

const INITIAL: ProgressConnection = {
  events: [],
  connected: false,
  error: null,
  terminal: false,
};

const BACKOFF_BASE_MS = 1000;
const BACKOFF_MAX_MS = 30000;

/** Opciones del cliente SSE (backoff configurable para pruebas). */
export interface ProgressHookOptions {
  backoffBaseMs?: number;
  backoffMaxMs?: number;
}

/**
 * Cliente SSE del progreso de generación Enterprise.
 *
 * <p>Conecta a {@code GET /enterprise/{projectId}/{version}/stream} mediante
 * {@code fetch} + {@code ReadableStream} (envía el token JWT en la cabecera
 * Authorization). Maneja heartbeats (comentarios SSE ignorados), detecta la
 * desconexión y reconecta con backoff exponencial (1s, 2s, 4s… hasta 30s).
 * Detiene la reconexión cuando se alcanza un estado terminal (COMPLETED o
 * FAILED).</p>
 */
export function useEnterpriseProgress(
  projectId: string | null,
  version: number,
  options: ProgressHookOptions = {},
): ProgressConnection {
  const backoffBaseMs = options.backoffBaseMs ?? BACKOFF_BASE_MS;
  const backoffMaxMs = options.backoffMaxMs ?? BACKOFF_MAX_MS;
  const [connection, setConnection] = useState<ProgressConnection>(INITIAL);
  const terminalRef = useRef(false);

  useEffect(() => {
    if (!projectId) {
      return;
    }
    let cancelled = false;
    terminalRef.current = false;
    const controller = new AbortController();
    let retries = 0;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const connect = async (): Promise<void> => {
      if (cancelled || controller.signal.aborted) return;
      try {
        const res = await fetch(
          `${API_URL}/enterprise/${projectId}/${version}/stream`,
          {
            headers: { ...authHeaders(), Accept: "text/event-stream" },
            signal: controller.signal,
          },
        );
        if (!res.ok || !res.body) {
          throw new Error(`SSE request failed (${res.status})`);
        }
        retries = 0;
        setConnection((c) => ({ ...c, connected: true, error: null }));
        await readStream(res.body, (event) => {
          if (cancelled) return;
          setConnection((c) => ({ ...c, events: [...c.events, event] }));
          if (TERMINAL_STATES.includes(event.state)) {
            terminalRef.current = true;
            setConnection((c) => ({ ...c, terminal: true, connected: false }));
            controller.abort();
          }
        });
        if (!cancelled && !terminalRef.current) {
          scheduleReconnect();
        }
      } catch (err) {
        if (cancelled || controller.signal.aborted) return;
        setConnection((c) => ({
          ...c,
          connected: false,
          error: err instanceof Error ? err.message : String(err),
        }));
        if (!terminalRef.current) {
          scheduleReconnect();
        }
      }
    };

    const scheduleReconnect = (): void => {
      if (cancelled || terminalRef.current || controller.signal.aborted) return;
      const delay = Math.min(backoffMaxMs, backoffBaseMs * 2 ** retries);
      retries += 1;
      timer = setTimeout(() => {
        void connect();
      }, delay);
    };

    setConnection(INITIAL);
    void connect();

    return () => {
      cancelled = true;
      if (timer !== undefined) {
        clearTimeout(timer);
      }
      controller.abort();
    };
  }, [projectId, version, backoffBaseMs, backoffMaxMs]);

  return connection;
}

/** Lee un flujo SSE y emite cada evento de progreso parseado. */
export async function readStream(
  body: ReadableStream<Uint8Array>,
  onEvent: (event: EnterpriseProgressEvent) => void,
): Promise<void> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  for (;;) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split("\n\n");
    buffer = blocks.pop() ?? "";
    for (const block of blocks) {
      parseBlock(block, onEvent);
    }
  }
}

/** Parsea un bloque SSE ({@code event:…}/{@code data:…}) e invoca el callback. */
export function parseBlock(
  block: string,
  onEvent: (event: EnterpriseProgressEvent) => void,
): void {
  let eventName = "";
  const dataLines: string[] = [];
  for (const line of block.split("\n")) {
    if (line.startsWith("event:")) {
      eventName = line.slice("event:".length).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice("data:".length).trim());
    }
  }
  if (eventName !== "progress" || dataLines.length === 0) {
    return;
  }
  const payload = dataLines.join("\n");
  try {
    onEvent(JSON.parse(payload) as EnterpriseProgressEvent);
  } catch {
    // Ignora eventos malformados.
  }
}
