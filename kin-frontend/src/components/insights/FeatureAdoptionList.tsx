"use client";

import type { FeatureUsage } from "@/services/intelligence/types";

export default function FeatureAdoptionList({ features }: { features: FeatureUsage[] }) {
  if (features.length === 0) {
    return <p className="text-sm text-neutral-500">Aún no hay datos de adopción de funciones.</p>;
  }
  const sorted = [...features].sort((a, b) => b.uses - a.uses);
  return (
    <ul className="space-y-2">
      {sorted.map((feature) => (
        <li key={feature.feature} className="flex items-center justify-between rounded-lg border border-neutral-200 px-4 py-2 text-sm">
          <span className="font-medium text-neutral-700">{feature.feature}</span>
          <span className="text-neutral-500">
            {feature.uses} uso{feature.uses === 1 ? "" : "s"}
            {feature.lastUsed ? ` · ${new Date(feature.lastUsed).toLocaleDateString()}` : ""}
          </span>
        </li>
      ))}
    </ul>
  );
}
