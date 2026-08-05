"use client";

import { useProductIntelligence } from "@/hooks/useProductIntelligence";
import StatCard from "@/components/insights/StatCard";
import FeatureAdoptionList from "@/components/insights/FeatureAdoptionList";
import EmptyState from "@/components/ui/EmptyState";

export default function AnalyticsPage() {
  const { intelligence } = useProductIntelligence("dashboard_analytics");
  const { usage, metrics, features } = intelligence;

  if (usage.dailyCount === 0 && usage.monthlyCount === 0) {
    return (
      <main className="flex-1 px-6 py-8 max-w-6xl mx-auto w-full">
        <h1 className="text-2xl font-bold tracking-tight">Analytics</h1>
        <EmptyState
          title="Aún no hay datos de uso"
          description="Usa KIN para que aquí se acumulen tus métricas."
        />
      </main>
    );
  }

  return (
    <main className="flex-1 px-6 py-8 max-w-6xl mx-auto w-full">
      <h1 className="text-2xl font-bold tracking-tight">Analytics</h1>
      <p className="mt-1 text-sm text-neutral-500">Uso y actividad de tu cuenta (datos locales).</p>

      <section className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Eventos hoy" value={String(usage.dailyCount)} />
        <StatCard label="Eventos semanales" value={String(usage.weeklyCount)} />
        <StatCard label="Eventos mensuales" value={String(usage.monthlyCount)} />
        <StatCard label="Días activos" value={String(usage.activeDays)} />
        <StatCard label="Mensajes IA" value={String(usage.messagesSent)} />
        <StatCard label="Tokens estimados" value={String(usage.estimatedTokens)} />
        <StatCard label="Costo estimado" value={`$${usage.estimatedCost.toFixed(5)}`} />
        <StatCard label="Proyectos creados" value={String(usage.projectsCreated)} />
        <StatCard label="Feedback 👍" value={String(usage.positiveFeedback)} />
        <StatCard label="Feedback 👎" value={String(usage.negativeFeedback)} />
        <StatCard label="Retención" value={`${metrics.retention}%`} />
        <StatCard label="Onboarding" value={`${metrics.onboardingCompletion}%`} />
      </section>

      <section className="mt-8">
        <h2 className="text-lg font-semibold text-neutral-900">Adopción de funciones</h2>
        <div className="mt-3">
          <FeatureAdoptionList features={features} />
        </div>
      </section>
    </main>
  );
}
