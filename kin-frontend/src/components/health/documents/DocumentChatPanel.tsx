"use client";

import { useEffect, useRef, useState } from "react";
import type { ClinicalDocument } from "@/services/documents";
import {
  documentChatService,
  VERIFICATION_LABELS,
  type DocumentChatMessage,
  type VerificationStatus,
} from "@/services/documentChat";
import DocumentAnalysisPdfButton from "@/components/health/documents/DocumentAnalysisPdfButton";

/** Mensaje canónico del botón "Importar información": extrae los datos relevantes del PDF. */
export const IMPORT_PROMPT =
  "Extrae los datos relevantes de este documento (por ejemplo: glucosa, colesterol, triglicéridos, " +
  "presión arterial u otros exámenes) con sus valores y unidades. Preséntalos de forma clara y " +
  "estructurada, marca los que estén fuera del rango normal y explícalos en lenguaje sencillo. " +
  "No inventes valores: usa únicamente el contenido del documento.";

type Props = {
  document: ClinicalDocument;
};

function formatTime(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function DocumentChatPanel({ document }: Props) {
  const [messages, setMessages] = useState<DocumentChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [lastVerification, setLastVerification] = useState<VerificationStatus>("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    documentChatService
      .history(document.id)
      .then((history) => {
        if (!cancelled) setMessages(history);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [document.id]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ behavior: "smooth" });
  }, [messages]);

  const sendMessage = async (content: string) => {
    const trimmed = content.trim();
    if (!trimmed || sending) return;
    const optimistic: DocumentChatMessage = {
      id: `tmp-${Date.now()}`,
      documentId: document.id,
      userId: "",
      role: "USER",
      content: trimmed,
      createdAt: new Date().toISOString(),
    };
    setSending(true);
    setError("");
    setNotice("");
    setInput("");
    setMessages((prev) => [...prev, optimistic]);
    try {
      const turn = await documentChatService.send(document.id, trimmed);
      setMessages((prev) => [
        ...prev.filter((m) => !m.id.startsWith("tmp-")),
        turn.userMessage,
        turn.assistantMessage,
      ]);
      setLastVerification(turn.verificationStatus ?? "");
    } catch (err) {
      setMessages((prev) => prev.filter((m) => m.id !== optimistic.id));
      setError((err as Error).message);
    } finally {
      setSending(false);
    }
  };

  const handleImport = () => {
    if (!document.analyzable || sending) return;
    sendMessage(IMPORT_PROMPT);
  };

  const handleClear = async () => {
    if (messages.length === 0) return;
    setError("");
    setNotice("");
    try {
      await documentChatService.clear(document.id);
      setMessages([]);
      setLastVerification("");
      setNotice("Conversación eliminada.");
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const verificationNote = lastVerification ? VERIFICATION_LABELS[lastVerification] ?? "" : "";

  return (
    <section className="rounded-xl border border-neutral-200 bg-white flex flex-col overflow-hidden">
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 border-b border-neutral-100">
        <div className="min-w-0">
          <h3 className="text-sm font-semibold text-neutral-800">
            Análisis con IA · {document.fileName}
          </h3>
          {!document.analyzable ? (
            <p className="text-xs text-amber-700">
              No se pudo extraer texto de este documento (¿PDF escaneado?). El análisis automático no
              está disponible; puedes descargar el original.
            </p>
          ) : (
            <p className="text-xs text-neutral-500">
              Conversación contextual sobre el documento. Interpretación automática de apoyo.
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {document.analyzable && (
            <button
              type="button"
              onClick={handleImport}
              disabled={sending}
              className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700 transition disabled:opacity-60"
            >
              Importar información
            </button>
          )}
          <DocumentAnalysisPdfButton document={document} messages={messages} />
          {messages.length > 0 && (
            <button
              type="button"
              onClick={handleClear}
              className="rounded-lg border border-neutral-200 px-2.5 py-1.5 text-xs font-medium text-neutral-500 hover:bg-neutral-50 transition"
            >
              Borrar conversación
            </button>
          )}
        </div>
      </div>

      {error && <p className="mx-4 mt-3 text-xs text-red-600 bg-red-50 px-3 py-2 rounded-lg">{error}</p>}
      {notice && (
        <p className="mx-4 mt-3 text-xs text-emerald-700 bg-emerald-50 px-3 py-2 rounded-lg">{notice}</p>
      )}
      {verificationNote && (
        <p className="mx-4 mt-3 text-xs text-sky-800 bg-sky-50 px-3 py-2 rounded-lg">
          {verificationNote}
        </p>
      )}

      <div className="px-4 py-2 text-xs text-neutral-500 border-b border-neutral-100">
        ⚠️ Interpretación automática. No sustituye la consulta con un profesional de la salud.
      </div>

      <div className="flex-1 min-h-[260px] max-h-[420px] overflow-y-auto p-4 flex flex-col gap-3">
        {loading ? (
          <p className="text-xs text-neutral-400">Cargando conversación...</p>
        ) : messages.length === 0 ? (
          <div className="text-center py-8">
            {!document.analyzable ? (
              <p className="text-xs text-neutral-400">
                Sube un PDF con texto (p. ej. un informe de laboratorio) para analizarlo.
              </p>
            ) : (
              <>
                <p className="text-sm font-medium text-neutral-600">
                  Pregunta sobre este documento o pulsa “Importar información”.
                </p>
                <p className="text-xs text-neutral-400 mt-1">
                  Ej.: “¿Qué significa mi nivel de glucosa?” · “Explícame este examen en lenguaje sencillo”.
                </p>
              </>
            )}
          </div>
        ) : (
          messages.map((msg) => {
            const isUser = msg.role === "USER";
            return (
              <div key={msg.id} className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
                <div
                  className={`max-w-[85%] rounded-2xl px-3 py-2 text-sm whitespace-pre-wrap ${
                    isUser
                      ? "bg-primary-600 text-white rounded-br-sm"
                      : "bg-neutral-100 text-neutral-800 rounded-bl-sm"
                  }`}
                >
                  <p className={`text-[10px] mb-0.5 ${isUser ? "text-primary-100" : "text-neutral-400"}`}>
                    {isUser ? "Tú" : "Asistente KIN"} · {formatTime(msg.createdAt)}
                  </p>
                  {msg.content}
                </div>
              </div>
            );
          })
        )}
        {sending && (
          <div className="flex justify-start">
            <div className="rounded-2xl rounded-bl-sm bg-neutral-100 px-4 py-2 text-xs text-neutral-500">
              El asistente está analizando el documento…
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {document.analyzable && (
        <form
          className="border-t border-neutral-100 p-3 flex items-end gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            sendMessage(input);
          }}
        >
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            rows={2}
            placeholder="Escribe tu pregunta sobre el documento…"
            aria-label="Pregunta sobre el documento"
            className="flex-1 resize-none rounded-xl border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          <button
            type="submit"
            disabled={sending || !input.trim()}
            className="rounded-xl bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
          >
            Enviar
          </button>
        </form>
      )}
    </section>
  );
}
