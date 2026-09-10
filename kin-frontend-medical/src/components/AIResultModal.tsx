"use client";

import { useState } from "react";

type AIAssistType = "SUMMARY" | "ORGANIZE" | "PREPARE" | "EXPLAIN" | "DRAFT";

interface AIResultModalProps {
  open: boolean;
  onClose: () => void;
  type: AIAssistType;
  title: string;
  response: string;
  onCopy?: () => void;
}

export default function AIResultModal({ open, onClose, type, title, response, onCopy }: AIResultModalProps) {
  const [showCopy, setShowCopy] = useState(false);

  const copyToClipboard = () => {
    navigator.clipboard.writeText(response).then(() => setShowCopy(true));
  };

  const typeLabel: Record<AIAssistType, string> = {
    SUMMARY: "Resumen",
    ORGANIZE: "Organizar síntomas",
    PREPARE: "Preparar consulta",
    EXPLAIN: "Explicar diagnóstico",
    DRAFT: "Redactar mensaje",
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/40 transition-opacity" onClick={onClose} />
      <div className="relative z-10 w-full max-w-2xl mx-4 rounded-2xl border border-neutral-200 bg-white p-6 shadow-xl max-h-[80vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-semibold text-neutral-900">{title}</h3>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 hover:bg-neutral-100 transition"
            aria-label="Cerrar modal"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <p className="text-sm text-neutral-500 mb-4">{typeLabel[type]}</p>

        <pre
          className="bg-neutral-50 rounded-lg p-4 text-sm font-mono text-neutral-900 overflow-auto break-words"
        >
          {response}
        </pre>

        {onCopy && (
          <div className="mt-4 flex gap-2">
            <button
              onClick={copyToClipboard}
              className="rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-dark transition"
              disabled={showCopy}
            >
              Copiar
            </button>
            {showCopy && (
              <span className="text-xs text-neutral-400">Copiado al portapapeles</span>
            )}
          </div>
        )}

        <div className="mt-6 pt-4 border-t border-neutral-200">
          <button
            onClick={onClose}
            className="w-full rounded-lg bg-neutral-100 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-200 transition"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}