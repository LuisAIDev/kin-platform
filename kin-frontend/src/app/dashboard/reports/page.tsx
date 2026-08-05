"use client";

import { useProductIntelligence } from "@/hooks/useProductIntelligence";
import StatCard from "@/components/insights/StatCard";
import EnterpriseReportGenerator from "@/components/insights/EnterpriseReportGenerator";

export default function ReportsPage() {
  const { intelligence } = useProductIntelligence("dashboard_reports");
  const { usage, metrics, insights } = intelligence;

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <h1 className="text-2xl font-bold tracking-tight">Reportes</h1>
      <p className="mt-1 text-sm text-neutral-500">Exporta la inteligencia de tu cuenta en JSON, CSV o PDF.</p>

      <section className="mt-6 grid gap-4 sm:grid-cols-3">
        <StatCard label="Uso total (30 días)" value={String(usage.monthlyCount)} />
        <StatCard label="Mensajes IA" value={String(usage.messagesSent)} />
        <StatCard label="Onboarding" value={`${metrics.onboardingCompletion}%`} />
        <StatCard label="Satisfacción" value={`${insights.satisfaction}%`} />
        <StatCard label="Retención" value={`${metrics.retention}%`} />
        <StatCard label="Costo estimado" value={`$${usage.estimatedCost.toFixed(5)}`} />
      </section>

      <div className="mt-8">
        <h2 className="text-lg font-semibold text-neutral-900">Exportar</h2>
        <div className="mt-3">
          <EnterpriseReportGenerator intelligence={intelligence} />
        </div>
      </div>
    </main>
  );
}
