"use client";

import type { ClinicalAlert } from "@/services/physician";

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function AlertList({
  alerts,
  onAcknowledge,
}: {
  alerts: ClinicalAlert[];
  onAcknowledge: (alertId: string) => void;
}) {
  if (alerts.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-6 text-sm text-neutral-500">
        No hay alertas activas.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {alerts.map((alert) => (
        <div
          key={alert.id}
          className="rounded-xl border border-red-200 bg-red-50 p-4 flex items-start justify-between gap-3"
        >
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-red-600 text-white px-2.5 py-0.5 text-xs font-bold uppercase">
                {alert.severity}
              </span>
              <span className="text-sm font-semibold text-red-900">
                Triaje de alta urgencia
              </span>
            </div>
            <p className="text-sm text-red-800">{alert.message}</p>
            <span className="text-xs text-red-400">{formatDate(alert.createdAt)}</span>
          </div>
          <button
            type="button"
            onClick={() => onAcknowledge(alert.id)}
            className="rounded-lg bg-white border border-red-300 text-red-700 px-3 py-1.5 text-xs font-medium hover:bg-red-100 transition shrink-0"
          >
            Marcar atendida
          </button>
        </div>
      ))}
    </div>
  );
}
