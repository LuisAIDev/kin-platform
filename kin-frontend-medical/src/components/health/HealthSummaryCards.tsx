"use client";

import type { HealthSummary } from "@/services/dashboard";

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function HealthSummaryCards({ summary }: { summary: HealthSummary }) {
  const cards = [
    { label: "Consultas de triaje", value: String(summary.totalConsultations) },
    { label: "Diagnósticos diferenciales", value: String(summary.totalDifferentials) },
    { label: "Último triaje", value: formatDate(summary.lastTriageAt) },
    { label: "Recordatorios activos", value: String(summary.activeReminders) },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <div
          key={card.label}
          className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-1"
        >
          <span className="text-xs font-medium uppercase text-neutral-400">{card.label}</span>
          <span className="text-2xl font-bold text-primary-700">{card.value}</span>
        </div>
      ))}

      {summary.topConditions.length > 0 && (
        <div className="sm:col-span-2 lg:col-span-4 rounded-xl border border-neutral-200 bg-white p-5">
          <h2 className="text-sm font-semibold mb-3">Condiciones más frecuentes</h2>
          <div className="flex flex-wrap gap-2">
            {summary.topConditions.map((c) => (
              <span
                key={c.name}
                className="rounded-full bg-primary-50 text-primary-700 px-3 py-1.5 text-sm font-medium"
              >
                {c.name} · {c.occurrences} {c.occurrences === 1 ? "vez" : "veces"}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
