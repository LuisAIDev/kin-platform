"use client";

import Link from "next/link";
import type { Recommendation } from "@/services/intelligence/types";

export default function RecommendationList({ recommendations }: { recommendations: Recommendation[] }) {
  if (recommendations.length === 0) {
    return <p className="text-sm text-neutral-500">Sin recomendaciones por ahora.</p>;
  }
  return (
    <ul className="space-y-3">
      {recommendations.map((rec) => (
        <li key={rec.id} className="rounded-xl border border-neutral-200 bg-white p-4">
          <h3 className="text-sm font-semibold text-neutral-900">{rec.title}</h3>
          <p className="mt-1 text-sm text-neutral-500">{rec.description}</p>
          {rec.href && (
            <Link
              href={rec.href}
              className="mt-2 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
            >
              Ir →
            </Link>
          )}
        </li>
      ))}
    </ul>
  );
}
