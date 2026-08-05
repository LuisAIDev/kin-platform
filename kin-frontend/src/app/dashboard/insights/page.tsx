"use client";

import { useProductIntelligence } from "@/hooks/useProductIntelligence";
import StatCard from "@/components/insights/StatCard";
import TimelineView from "@/components/insights/TimelineView";

export default function InsightsPage() {
  const { intelligence } = useProductIntelligence("dashboard_insights");
  const { insights, timeline } = intelligence;

  return (
    <main className="flex-1 px-6 py-8 max-w-6xl mx-auto w-full">
      <h1 className="text-2xl font-bold tracking-tight">Insights</h1>
      <p className="mt-1 text-sm text-neutral-500">Análisis determinista de tus conversaciones y actividad.</p>

      <section className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Longitud promedio" value={`${insights.averageLength} chars`} />
        <StatCard label="Preguntas" value={`${insights.questionsPerSession}%`} />
        <StatCard label="Duración" value={`${(insights.durationMs / 60000).toFixed(1)} min`} />
        <StatCard label="Intención predominante" value={insights.predominantIntent} />
        <StatCard label="Calidad de respuestas" value={`${insights.responseQuality}%`} />
        <StatCard label="Satisfacción" value={`${insights.satisfaction}%`} />
      </section>

      <section className="mt-8 grid gap-6 lg:grid-cols-2">
        <div>
          <h2 className="text-lg font-semibold text-neutral-900">Temas frecuentes</h2>
          <ul className="mt-3 space-y-1">
            {insights.frequentTopics.length === 0
              ? <li className="text-sm text-neutral-500">Sin temas registrados.</li>
              : insights.frequentTopics.map((topic) => (
                <li key={topic} className="rounded-lg border border-neutral-200 px-3 py-2 text-sm text-neutral-700">
                  {topic}
                </li>
              ))}
          </ul>
        </div>
        <div>
          <h2 className="text-lg font-semibold text-neutral-900">Línea de tiempo de sesión</h2>
          <div className="mt-3">
            <TimelineView events={timeline} />
          </div>
        </div>
      </section>
    </main>
  );
}
