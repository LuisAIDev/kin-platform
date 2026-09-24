"use client";

import { useEffect, useState } from "react";

type InstitutionalKpis = {
  branchCount?: number;
  totalMembers?: number;
  activeMembers?: number;
  membersByRole?: Record<string, number>;
  billing?: {
    totalFacturadoCop?: number;
    recaudadoCop?: number;
    totalCarteraCop?: number;
    carteraVencidaCop?: number;
    totalGlosasCop?: number;
    ripsPendientes?: number;
    fevAceptadas?: number;
    fevRechazadas?: number;
  };
};

export default function InstitutionalKpisPage() {
  const [kpis, setKpis] = useState<InstitutionalKpis | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/v1/institutional/kpis", { credentials: "include" })
      .then((r) => {
        if (!r.ok) throw new Error("No se pudieron cargar los KPIs");
        return r.json();
      })
      .then(setKpis)
      .catch((e) => setError(e instanceof Error ? e.message : "Error"));
  }, []);

  if (error) return <main className="mx-auto max-w-4xl px-4 py-8"><p className="text-sm text-red-600">{error}</p></main>;
  if (!kpis) return <main className="mx-auto max-w-4xl px-4 py-8"><p className="text-sm text-neutral-500">Cargando…</p></main>;

  const billing = kpis.billing ?? {};
  const money = (v?: number) => `$${Number(v ?? 0).toLocaleString("es-CO")}`;

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold tracking-tight text-neutral-900">KPIs institucionales</h1>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card label="Sedes" value={String(kpis.branchCount ?? 0)} />
        <Card label="Miembros totales" value={String(kpis.totalMembers ?? 0)} />
        <Card label="Miembros activos" value={String(kpis.activeMembers ?? 0)} />
      </div>

      <h2 className="mt-8 text-lg font-semibold text-neutral-900">Facturación</h2>
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card label="Facturado" value={money(billing.totalFacturadoCop)} />
        <Card label="Recaudado" value={money(billing.recaudadoCop)} />
        <Card label="Cartera" value={money(billing.totalCarteraCop)} />
        <Card label="Cartera vencida" value={money(billing.carteraVencidaCop)} />
        <Card label="Glosas" value={money(billing.totalGlosasCop)} />
        <Card label="RIPS pendientes" value={String(billing.ripsPendientes ?? 0)} />
        <Card label="FEV aceptadas" value={String(billing.fevAceptadas ?? 0)} />
        <Card label="FEV rechazadas" value={String(billing.fevRechazadas ?? 0)} />
      </div>

      {kpis.membersByRole && Object.keys(kpis.membersByRole).length > 0 && (
        <>
          <h2 className="mt-8 text-lg font-semibold text-neutral-900">Miembros por rol</h2>
          <ul className="mt-4 divide-y divide-neutral-200 rounded-xl border border-neutral-200 bg-white">
            {Object.entries(kpis.membersByRole).map(([role, count]) => (
              <li key={role} className="flex items-center justify-between p-4">
                <span className="text-neutral-700">{role}</span>
                <span className="font-semibold text-neutral-900">{count}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-4">
      <p className="text-xs uppercase tracking-wide text-neutral-500">{label}</p>
      <p className="mt-1 text-xl font-semibold text-neutral-900">{value}</p>
    </div>
  );
}
