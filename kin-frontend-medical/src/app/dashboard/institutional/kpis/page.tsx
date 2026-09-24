"use client";

import { useEffect, useState } from "react";

type Kpis = {
  totalFacturadoCop?: number;
  totalGlosasCop?: number;
  glosaRate?: number;
  totalCarteraCop?: number;
  carteraVencidaCop?: number;
  ripsPendientes?: number;
  fevAceptadas?: number;
  fevRechazadas?: number;
  recaudadoCop?: number;
};

const LABELS: { key: keyof Kpis; label: string; money?: boolean }[] = [
  { key: "totalFacturadoCop", label: "Total facturado", money: true },
  { key: "recaudadoCop", label: "Recaudado", money: true },
  { key: "totalCarteraCop", label: "Cartera total", money: true },
  { key: "carteraVencidaCop", label: "Cartera vencida", money: true },
  { key: "totalGlosasCop", label: "Glosas", money: true },
  { key: "ripsPendientes", label: "RIPS pendientes" },
  { key: "fevAceptadas", label: "FEV aceptadas" },
  { key: "fevRechazadas", label: "FEV rechazadas" },
];

export default function InstitutionalKpisPage() {
  const [kpis, setKpis] = useState<Kpis | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/v1/billing/dashboard/kpis", { credentials: "include" })
      .then((r) => {
        if (!r.ok) throw new Error("No se pudieron cargar los KPIs");
        return r.json();
      })
      .then(setKpis)
      .catch((e) => setError(e instanceof Error ? e.message : "Error"));
  }, []);

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold tracking-tight text-neutral-900">KPIs institucionales</h1>
      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}
      {!kpis && !error && <p className="mt-4 text-sm text-neutral-500">Cargando…</p>}
      {kpis && (
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {LABELS.map(({ key, label, money }) => (
            <div key={key} className="rounded-xl border border-neutral-200 bg-white p-4">
              <p className="text-xs uppercase tracking-wide text-neutral-500">{label}</p>
              <p className="mt-1 text-xl font-semibold text-neutral-900">
                {money
                  ? `$${Number(kpis[key] ?? 0).toLocaleString("es-CO")}`
                  : String(kpis[key] ?? 0)}
              </p>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
