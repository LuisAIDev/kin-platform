"use client";

import type { PhysicianPatientSummary } from "@/services/physician";
import type { TriageHistoryEntry } from "@/services/triage";

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function PatientDetailView({
  summary,
  history,
  onClose,
}: {
  summary: PhysicianPatientSummary | null;
  history: TriageHistoryEntry[];
  onClose: () => void;
}) {
  if (!summary) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4 overflow-y-auto">
      <div
        className="bg-white rounded-2xl max-w-2xl w-full p-6 flex flex-col gap-5 my-8"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between">
          <div>
            <h3 className="text-lg font-semibold">{summary.patientName}</h3>
            <p className="text-sm text-neutral-500 mt-0.5">
              Resumen clínico · {summary.totalTriages} triaje
              {summary.totalTriages === 1 ? "" : "s"}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="text-neutral-400 hover:text-neutral-600 text-xl leading-none"
          >
            ×
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="rounded-lg border border-neutral-100 p-3">
            <span className="text-xs font-semibold uppercase text-neutral-400">Último triaje</span>
            <p className="text-sm mt-1">{formatDate(summary.lastTriageAt)}</p>
          </div>
          <div className="rounded-lg border border-neutral-100 p-3">
            <span className="text-xs font-semibold uppercase text-neutral-400">Factores de riesgo</span>
            <p className="text-sm mt-1">{summary.riskFactors.join(", ") || "—"}</p>
          </div>
          <div className="rounded-lg border border-neutral-100 p-3">
            <span className="text-xs font-semibold uppercase text-neutral-400">Crónicas</span>
            <p className="text-sm mt-1">{summary.chronicConditions.join(", ") || "—"}</p>
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <span className="text-xs font-semibold uppercase text-neutral-400">
            Condiciones activas
          </span>
          <div className="flex flex-wrap gap-2">
            {summary.activeConditions.map((c) => (
              <span key={c} className="rounded-full bg-primary-50 text-primary-700 px-3 py-1 text-xs font-medium">
                {c}
              </span>
            ))}
            {summary.activeConditions.length === 0 && (
              <span className="text-sm text-neutral-400">—</span>
            )}
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-xs font-semibold uppercase text-neutral-400">Historial</span>
          {history.length === 0 && (
            <p className="text-sm text-neutral-400">Sin consultas registradas.</p>
          )}
          {history.slice(0, 5).map((entry) => (
            <div key={entry.id} className="rounded-lg border border-neutral-100 p-3 flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <span className="text-xs text-neutral-400">{formatDate(entry.createdAt)}</span>
                <span className="text-xs text-neutral-500">
                  {entry.results[0]?.condition ?? "Sin resultados"}
                </span>
              </div>
              <p className="text-xs text-neutral-500">{entry.symptoms.join(", ")}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
