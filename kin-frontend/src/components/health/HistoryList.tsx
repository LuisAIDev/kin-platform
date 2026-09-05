"use client";

import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "@/services/triage";
import { API_URL } from "@/services/api";

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function HistoryList({
  page,
  onPageChange,
  onSelect,
  onShare,
}: {
  page: PageResponse<TriageHistoryEntry>;
  onPageChange: (page: number) => void;
  onSelect: (entry: TriageHistoryEntry) => void;
  onShare?: (entry: TriageHistoryEntry) => void;
}) {
  const { content, totalElements, totalPages, currentPage } = page;

  if (content.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-500">
        Aún no hay consultas de triaje.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-xl border border-neutral-200 bg-white overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-neutral-50 text-left text-xs uppercase text-neutral-400">
            <tr>
              <th className="px-4 py-3 font-medium">Fecha</th>
              <th className="px-4 py-3 font-medium">Síntomas</th>
              <th className="px-4 py-3 font-medium">Condiciones sugeridas</th>
              <th className="px-4 py-3 font-medium">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100">
            {content.map((entry) => (
              <tr key={entry.id} className="hover:bg-neutral-50">
                <td className="px-4 py-3 whitespace-nowrap text-neutral-600">
                  {formatDate(entry.createdAt)}
                </td>
                <td className="px-4 py-3 text-neutral-700">
                  {entry.symptoms.slice(0, 3).join(", ")}
                  {entry.symptoms.length > 3 ? "…" : ""}
                </td>
                <td className="px-4 py-3 text-neutral-700">
                  {entry.results.slice(0, 2).map((r) => r.condition).join(", ")}
                  {entry.results.length > 2 ? "…" : ""}
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex gap-2 justify-end">
                    {onShare && (
                      <button
                        type="button"
                        onClick={() => onShare(entry)}
                        className="rounded-lg border border-violet-200 text-violet-700 px-3 py-1.5 text-xs font-medium hover:bg-violet-50 transition"
                        title="Compartir el informe con un médico externo"
                      >
                        Compartir
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => onSelect(entry)}
                      className="rounded-lg border border-primary-200 text-primary-700 px-3 py-1.5 text-xs font-medium hover:bg-primary-50 transition"
                    >
                      Ver detalle
                    </button>
                    <button
                      type="button"
                      onClick={() =>
                        window.open(
                          `${API_URL}/health/triage/${entry.id}/export/pdf`,
                          "_blank"
                        )
                      }
                      className="rounded-lg border border-success-200 text-success-700 px-3 py-1.5 text-xs font-medium hover:bg-success-50 transition"
                      title="Descargar informe PDF"
                    >
                      PDF
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-neutral-500">
          <span>
            {totalElements} consulta{totalElements === 1 ? "" : "s"} · Página{" "}
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
      )}
    </div>
  );
}