"use client";

import { useMemo } from "react";
import type { TriageHistoryEntry } from "@/services/triage";

/**
 * Gráfico de barras simple de la evolución de consultas de triaje por mes
 * (ADR-030, mejora UX). Se calcula de forma determinista en el cliente a
 * partir del historial; sin librerías de gráficos.
 */
export default function SymptomEvolutionChart({
  history,
}: {
  history: TriageHistoryEntry[];
}) {
  const byMonth = useMemo(() => {
    const map = new Map<string, number>();
    for (const entry of history) {
      const key = new Date(entry.createdAt).toLocaleDateString("es-ES", {
        month: "short",
        year: "2-digit",
      });
      map.set(key, (map.get(key) ?? 0) + 1);
    }
    return Array.from(map.entries())
      .sort((a, b) => a[0].localeCompare(b[0], "es"))
      .slice(-6);
  }, [history]);

  if (byMonth.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-6 text-sm text-neutral-500">
        Aún no hay suficientes consultas para mostrar la evolución.
      </div>
    );
  }

  const max = Math.max(...byMonth.map(([, count]) => count), 1);

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-6">
      <h2 className="text-sm font-semibold mb-4">Evolución de consultas</h2>
      <div className="flex items-end gap-3 h-28">
        {byMonth.map(([month, count]) => (
          <div key={month} className="flex flex-col items-center gap-1 flex-1">
            <span className="text-xs text-neutral-500 font-medium">{count}</span>
            <div
              className="w-full max-w-10 rounded-t bg-primary-600"
              style={{ height: `${Math.max(4, (count / max) * 80)}px` }}
              aria-label={`${month}: ${count} consultas`}
            />
            <span className="text-[10px] text-neutral-400 capitalize">{month}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
