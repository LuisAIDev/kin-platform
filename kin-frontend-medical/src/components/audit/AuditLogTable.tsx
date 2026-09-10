"use client";

import type { AuditLogEntry } from "@/services/audit";

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
 * Tabla de registros de auditoría (ADR-035). Muestra acción, usuario, recurso,
 * paciente y fecha; opcionalmente IP y detalles.
 */
export default function AuditLogTable({ entries, showUser = false }: { entries: AuditLogEntry[]; showUser?: boolean }) {
  if (entries.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-neutral-300 p-8 text-center text-sm text-neutral-500">
        Sin registros de auditoría para estos criterios.
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-neutral-200 bg-white overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="bg-neutral-50 text-left text-xs uppercase text-neutral-400">
          <tr>
            <th className="px-4 py-3 font-medium">Fecha</th>
            <th className="px-4 py-3 font-medium">Acción</th>
            {showUser && <th className="px-4 py-3 font-medium">Usuario</th>}
            <th className="px-4 py-3 font-medium">Recurso</th>
            <th className="px-4 py-3 font-medium">Paciente</th>
            <th className="px-4 py-3 font-medium">IP</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-neutral-100">
          {entries.map((entry) => (
            <tr key={entry.id} className="hover:bg-neutral-50">
              <td className="px-4 py-3 text-neutral-600 whitespace-nowrap">{formatDate(entry.timestamp)}</td>
              <td className="px-4 py-3">
                <span className="rounded-full bg-blue-100 text-blue-700 px-2.5 py-0.5 text-xs font-bold">
                  {entry.action}
                </span>
              </td>
              {showUser && <td className="px-4 py-3 text-neutral-700 font-mono text-xs">{entry.userId}</td>}
              <td className="px-4 py-3 text-neutral-600">{entry.resourceType}</td>
              <td className="px-4 py-3 text-neutral-600 font-mono text-xs">
                {entry.patientId ? entry.patientId.slice(0, 8) : "—"}
              </td>
              <td className="px-4 py-3 text-neutral-400 font-mono text-xs">{entry.ipAddress || "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
