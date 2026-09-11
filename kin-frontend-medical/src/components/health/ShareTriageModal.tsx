"use client";

import { useEffect, useState } from "react";
import { triageShareService, type TriageShareResponse } from "@/services/triageShare";

function formatExpiry(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ShareTriageModal({
  triageId,
  onClose,
}: {
  triageId: string;
  onClose: () => void;
}) {
  const [loading, setLoading] = useState(true);
  const [share, setShare] = useState<TriageShareResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    triageShareService
      .createShare(triageId)
      .then((data) => {
        if (!cancelled) setShare(data);
      })
      .catch((err) => {
        if (cancelled) return;
        const apiError = err as Error & { code?: string };
        if (apiError.code === "QUOTA_EXCEEDED") {
          setError("Compartir informes requiere el plan Personal+.");
        } else {
          setError(apiError.message);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [triageId]);

  const handleCopy = async () => {
    if (!share) return;
    try {
      await navigator.clipboard.writeText(share.url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard API no disponible: se deja el enlace visible para copiar manualmente.
    }
  };

  const handleRevoke = async () => {
    setError("");
    try {
      await triageShareService.revokeShare(triageId);
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl max-w-lg w-full p-6 flex flex-col gap-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between">
          <h3 className="text-lg font-semibold">Compartir informe de triaje</h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="text-neutral-400 hover:text-neutral-600 text-xl leading-none"
          >
            ×
          </button>
        </div>

        {error && (
          <div className="flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3">
            <p className="text-sm text-red-700">{error}</p>
            <a
              href="/dashboard/patient/plans"
              className="rounded-lg bg-primary-600 px-4 py-2 text-center text-sm font-medium text-white hover:bg-primary-700 transition"
            >
              Ver planes
            </a>
          </div>
        )}

        {loading && !error && <p className="text-sm text-neutral-500">Generando enlace...</p>}

        {share && !error && (
          <>
            <p className="text-sm text-neutral-600">
              Este enlace da acceso de solo lectura al informe de tu triaje a
              cualquier persona que lo reciba, durante 24 horas.
            </p>

            <div className="flex flex-col gap-2 rounded-lg border border-neutral-200 bg-neutral-50 p-3">
              <span className="text-xs font-semibold uppercase text-neutral-400">
                Enlace de compartición
              </span>
              <p className="break-all text-sm text-neutral-700">{share.url}</p>
              <p className="text-xs text-neutral-500">
                Expira el {formatExpiry(share.expiresAt)}.
              </p>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleCopy}
                className="flex-1 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition"
              >
                {copied ? "¡Copiado!" : "Copiar enlace"}
              </button>
              <button
                type="button"
                onClick={handleRevoke}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 transition"
              >
                Revocar
              </button>
            </div>
          </>
        )}

        {!share && !loading && !error && (
          <p className="text-sm text-neutral-500">No se pudo generar el enlace.</p>
        )}
      </div>
    </div>
  );
}
