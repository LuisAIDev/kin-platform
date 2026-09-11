import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  parseBlock,
  readStream,
  useEnterpriseProgress,
} from "@/hooks/useEnterpriseProgress";
import type { EnterpriseProgressEvent } from "@/types/enterprise";

function sseResponse(chunks: string[]): Response {
  const encoder = new TextEncoder();
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk));
      }
      controller.close();
    },
  });
  return new Response(stream, {
    status: 200,
    headers: { "Content-Type": "text/event-stream" },
  });
}

/** Flujo SSE que permanece abierto tras emitir los chunks (simula heartbeat). */
function sseOpenResponse(chunks: string[]): Response {
  const encoder = new TextEncoder();
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk));
      }
    },
  });
  return new Response(stream, {
    status: 200,
    headers: { "Content-Type": "text/event-stream" },
  });
}

describe("parseBlock", () => {
  it("parsea un bloque SSE de progreso", () => {
    const received: EnterpriseProgressEvent[] = [];
    parseBlock(
      'event:progress\ndata:{"projectId":"p1","version":1,"state":"RUNNING","timestamp":"2026-08-02T10:00:00Z"}',
      (event) => received.push(event),
    );
    expect(received).toHaveLength(1);
    expect(received[0].state).toBe("RUNNING");
  });

  it("ignora comentarios (heartbeat) y bloques de otro evento", () => {
    const received: EnterpriseProgressEvent[] = [];
    parseBlock(": heartbeat", received.push);
    parseBlock("event:other\ndata:{}", received.push);
    expect(received).toHaveLength(0);
  });

  it("ignora datos malformados", () => {
    const received: EnterpriseProgressEvent[] = [];
    parseBlock("event:progress\ndata:{not-json", received.push);
    expect(received).toHaveLength(0);
  });
});

describe("readStream", () => {
  it("lee los eventos de un flujo SSE completo", async () => {
    const received: EnterpriseProgressEvent[] = [];
    const body = sseResponse([
      'event:progress\ndata:{"projectId":"p1","version":1,"state":"REQUESTED","timestamp":"t1"}\n\n',
      'event:progress\ndata:{"projectId":"p1","version":1,"state":"COMPLETED","timestamp":"t2"}\n\n',
    ]).body!;
    await readStream(body, (event) => received.push(event));
    expect(received.map((e) => e.state)).toEqual(["REQUESTED", "COMPLETED"]);
  });

  it("maneja eventos fragmentados entre chunks", async () => {
    const received: EnterpriseProgressEvent[] = [];
    const encoder = new TextEncoder();
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(encoder.encode('event:progress\ndata:{"projectId":"p1","version":1,"state":"'));
        controller.enqueue(encoder.encode('RUNNING","timestamp":"t1"}\n\n'));
        controller.close();
      },
    });
    await readStream(stream, (event) => received.push(event));
    expect(received).toHaveLength(1);
    expect(received[0].state).toBe("RUNNING");
  });
});

describe("useEnterpriseProgress", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("conecta y acumula los eventos recibidos", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(sseResponse([
        'event:progress\ndata:{"projectId":"p1","version":1,"state":"REQUESTED","timestamp":"t1"}\n\n',
        'event:progress\ndata:{"projectId":"p1","version":1,"state":"RUNNING","timestamp":"t2"}\n\n',
      ]));

    const { result } = renderHook(() => useEnterpriseProgress("p1", 1));

    await waitFor(() => expect(result.current.events.length).toBe(2));
    expect(result.current.events[0].state).toBe("REQUESTED");
    expect(result.current.events[1].state).toBe("RUNNING");
    expect(result.current.connected).toBe(true);

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/enterprise/p1/1/stream");
  });

  it("detiene la reconexión al alcanzar un estado terminal", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event:progress\ndata:{"projectId":"p1","version":1,"state":"COMPLETED","timestamp":"t1"}\n\n',
    ]));

    const { result } = renderHook(() => useEnterpriseProgress("p1", 1));

    await waitFor(() => expect(result.current.terminal).toBe(true));
    expect(result.current.events[0].state).toBe("COMPLETED");
    expect(result.current.connected).toBe(false);
  });

  it("reconecta con backoff exponencial cuando el flujo se desconecta", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(sseResponse([]))
      .mockResolvedValue(sseOpenResponse([
        'event:progress\ndata:{"projectId":"p1","version":1,"state":"RUNNING","timestamp":"t1"}\n\n',
      ]));

    renderHook(() => useEnterpriseProgress("p1", 1, { backoffBaseMs: 10 }));

    // El primer flujo cierra sin eventos — se programa la reconexión (~10 ms).
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(2));
  });

  it("registra el error cuando la solicitud falla", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("network down"));

    const { result } = renderHook(() => useEnterpriseProgress("p1", 1));

    await waitFor(() => expect(result.current.error).toBe("network down"));
    expect(result.current.connected).toBe(false);
  });
});
