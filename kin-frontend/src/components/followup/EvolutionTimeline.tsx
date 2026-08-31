"use client";

import type { PatientEvolution } from "@/services/followup";

function formatDate(iso: string | null | undefined) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Línea de tiempo de la evolución del paciente (registros del médico).
 */
export default function EvolutionTimeline({ evolutions }: { evolutions: PatientEvolution[] }) {
  if (evolutions.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-neutral-300 p-8 text-center text-sm text-neutral-500">
        Aún no hay registros de evolución.
      </div>
    );
  }

  return (
    <ol className="flex flex-col gap-3">
      {evolutions.map((ev) => (
        <li key={ev.id} className="rounded-xl border border-neutral-200 bg-white p-4 flex flex-col gap-1.5">
          <div className="flex items-center justify-between">
            <p className="text-xs text-neutral-400">{formatDate(ev.recordedAt)}</p>
            {ev.medicationAdherence !== null && (
              <span
                className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${
                  ev.medicationAdherence
                    ? "bg-emerald-100 text-emerald-700"
                    : "bg-red-100 text-red-700"
                }`}
              >
                {ev.medicationAdherence ? "Cumple medicación" : "No cumple medicación"}
              </span>
            )}
          </div>
          {ev.symptoms && <p className="text-sm text-neutral-700">{ev.symptoms}</p>}
          {Object.entries(ev.vitals ?? {}).length > 0 && (
            <div className="flex flex-wrap gap-2">
              {Object.entries(ev.vitals ?? {}).map(([key, value]) => (
                <span key={key} className="rounded-lg bg-neutral-100 px-2.5 py-1 text-xs text-neutral-600">
                  {key}: <strong>{String(value)}</strong>
                </span>
              ))}
            </div>
          )}
          {ev.notes && <p className="text-xs text-neutral-500">{ev.notes}</p>}
        </li>
      ))}
    </ol>
  );
}
