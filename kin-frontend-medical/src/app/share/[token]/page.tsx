"use client";

import { use, useEffect, useState } from "react";
import { triageShareService, type SharedTriageContent } from "@/services/triageShare";

const SEVERITY_COLORS: Record<string, string> = {
  LEVE: "bg-emerald-100 text-emerald-800",
  MODERADO: "bg-amber-100 text-amber-800",
  GRAVE: "bg-red-100 text-red-800",
};

const URGENCY_COLORS: Record<string, string> = {
  BAJA: "bg-slate-100 text-slate-700",
  MEDIA: "bg-blue-100 text-blue-800",
  ALTA: "bg-orange-100 text-orange-800",
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

type Props = {
  params: Promise<{ token: string }>;
};

export default function SharedTriagePage({ params }: Props) {
  const { token } = use(params);
  const [content, setContent] = useState<SharedTriageContent | null>(null);
  const [invalid, setInvalid] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    triageShareService
      .getPublicContent(token)
      .then((data) => {
        if (!cancelled) setContent(data);
      })
      .catch(() => {
        if (!cancelled) setInvalid(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  if (loading) {
    return (
      <main className="min-h-screen bg-neutral-50 flex items-start justify-center px-6 pt-14 pb-16">
        <p className="text-sm text-neutral-500">Cargando informe...</p>
      </main>
    );
  }

  if (invalid || !content) {
    return (
      <main className="min-h-screen bg-neutral-50 flex items-center justify-center px-6">
        <div className="w-full max-w-md text-center flex flex-col gap-3">
          <div className="text-5xl">🔒</div>
          <h1 className="text-xl font-bold text-neutral-800">
            Enlace no válido o expirado
          </h1>
          <p className="text-sm text-neutral-500">
            Este enlace de compartición no es válido o ha expirado. Solicita al
            paciente un enlace nuevo.
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-neutral-50 flex flex-col items-center px-6 py-10">
      <div className="w-full max-w-3xl flex flex-col gap-4 print:max-w-none print:px-0 print:py-0">
        {/* Header del documento */}
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-medical-600 flex items-center justify-center shrink-0">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="4 7 12 12 4 17" />
                <polyline points="12 7 20 12 12 17" />
              </svg>
            </div>
            <span className="text-lg font-bold tracking-tight text-neutral-800">KIN Medical</span>
          </div>
          <button
            type="button"
            onClick={() => window.print()}
            className="rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 transition print:hidden"
          >
            Imprimir
          </button>
        </div>

        <div className="rounded-xl border border-neutral-200 bg-white p-6 sm:p-8 flex flex-col gap-5 shadow-sm">
          <div className="flex flex-col gap-1 border-b border-neutral-100 pb-4">
            <h1 className="text-2xl font-bold text-neutral-900">
              Informe de Triaje KIN
            </h1>
            <p className="text-sm text-neutral-500">
              Paciente: <span className="font-medium text-neutral-700">{content.patientName}</span>
            </p>
            <p className="text-xs text-neutral-400">
              Fecha del triaje: {formatDate(content.triageDate)}
            </p>
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold uppercase text-neutral-400">
              Motivo de consulta / Síntomas reportados
            </span>
            <div className="flex flex-wrap gap-2">
              {content.symptoms.length === 0 ? (
                <span className="text-sm text-neutral-500">No reportó síntomas.</span>
              ) : (
                content.symptoms.map((s) => (
                  <span key={s} className="rounded-full bg-neutral-100 px-3 py-1 text-sm text-neutral-700">
                    {s}
                  </span>
                ))
              )}
            </div>
          </div>

          <div className="flex flex-col gap-3">
            <span className="text-xs font-semibold uppercase text-neutral-400">
              Resultado del triaje
            </span>
            {content.conditions.length === 0 ? (
              <p className="text-sm text-neutral-500">
                No se identificaron condiciones candidatas.
              </p>
            ) : (
              content.conditions.map((c, i) => (
                <div key={i} className="rounded-xl border border-neutral-200 p-4 flex flex-col gap-2">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-base font-semibold text-neutral-800">{c.name}</p>
                      {c.description && (
                        <p className="text-sm text-neutral-500 mt-0.5">{c.description}</p>
                      )}
                    </div>
                    <span className="text-xl font-bold text-primary-700 shrink-0">
                      {Math.round(c.probability * 100)}%
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2 text-xs">
                    {c.severity && (
                      <span className={`rounded-full px-2.5 py-1 font-medium ${SEVERITY_COLORS[c.severity] ?? "bg-neutral-100 text-neutral-700"}`}>
                        Severidad: {c.severity}
                      </span>
                    )}
                    {c.urgency && (
                      <span className={`rounded-full px-2.5 py-1 font-medium ${URGENCY_COLORS[c.urgency] ?? "bg-neutral-100 text-neutral-700"}`}>
                        Urgencia: {c.urgency}
                      </span>
                    )}
                  </div>
                  {c.recommendation && (
                    <p className="text-sm text-neutral-600">{c.recommendation}</p>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        <div className="rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3 text-xs text-yellow-800">
          ⚠️ {content.disclaimer}
        </div>
      </div>
    </main>
  );
}
