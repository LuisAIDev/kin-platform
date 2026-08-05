"use client";

import { useProductIntelligence } from "@/hooks/useProductIntelligence";
import RecommendationList from "@/components/insights/RecommendationList";

export default function RecommendationsPage() {
  const { intelligence } = useProductIntelligence("dashboard_recommendations");

  return (
    <main className="flex-1 px-6 py-8 max-w-3xl mx-auto w-full">
      <h1 className="text-2xl font-bold tracking-tight">Recomendaciones</h1>
      <p className="mt-1 text-sm text-neutral-500">Sugerencias personalizadas basadas en tu uso.</p>
      <div className="mt-6">
        <RecommendationList recommendations={intelligence.recommendations} />
      </div>
    </main>
  );
}
