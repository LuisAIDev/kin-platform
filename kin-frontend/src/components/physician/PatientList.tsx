"use client";

import type { PageResponse } from "@/types";
import type { PhysicianPatientSummary } from "@/services/physician";

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function PatientList({
  page,
  onPageChange,
  onSelect,
}: {
  page: PageResponse<PhysicianPatientSummary>;
  onPageChange: (page: number) => void;
  onSelect: (patientId: string) => void;
}) {
  const { content, totalElements, totalPages, currentPage } = page;

  if (content.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-500">
        No tienes pacientes asignados.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-xl border border-neutral-200 bg-white overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-neutral-50 text-left text-xs uppercase text-neutral-400">
            <tr>
              <th className="px-4 py-3 font-medium">Paciente</th>
              <th className="px-4 py-3 font-medium">Último triaje</th>
              <th className="px-4 py-3 font-medium">Condiciones</th>
              <th className="px-4 py-3 font-medium">Alertas</th>
              <th className="px-4 py-3 font-medium"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100">
            {content.map((p) => (
              <tr key={p.patientId} className="hover:bg-neutral-50">
                <td className="px-4 py-3 font-medium text-neutral-800">{p.patientName}</td>
                <td className="px-4 py-3 text-neutral-600">{formatDate(p.lastTriageAt)}</td>
                <td className="px-4 py-3 text-neutral-600">
                  {p.activeConditions.slice(0, 2).join(", ") || "—"}
                </td>
                <td className="px-4 py-3">
                  {p.activeAlerts > 0 ? (
                    <span className="rounded-full bg-red-100 text-red-700 px-2.5 py-0.5 text-xs font-bold">
                      {p.activeAlerts}
                    </span>
                  ) : (
                    <span className="text-neutral-300">0</span>
                  )}
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    type="button"
                    onClick={() => onSelect(p.patientId)}
                    className="rounded-lg border border-primary-200 text-primary-700 px-3 py-1.5 text-xs font-medium hover:bg-primary-50 transition"
                  >
                    Ver resumen
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-neutral-500">
        <span>
          {totalElements} paciente{totalElements === 1 ? "" : "s"} · Página{" "}
          {currentPage + 1} de {Math.max(1, totalPages)}
        </span>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100 disabled:opacity-40"
          >
            Anterior
          </button>
          <button
            type="button"
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage + 1 >= totalPages}
            className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100 disabled:opacity-40"
          >
            Siguiente
          </button>
        </div>
      </div>
    </div>
  );
}
