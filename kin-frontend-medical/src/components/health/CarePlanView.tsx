"use client";

import type { CarePlan } from "@/services/dashboard";

const PRIORITY_COLORS: Record<string, string> = {
  ALTA: "bg-red-100 text-red-800",
  MEDIA: "bg-amber-100 text-amber-800",
  BAJA: "bg-slate-100 text-slate-700",
};

export default function CarePlanView({ plan }: { plan: CarePlan }) {
  if (plan.recommendations.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-6 text-sm text-neutral-500">
        Aún no hay recomendaciones de cuidado. Actualiza tu perfil o realiza un
        triaje para generar un plan personalizado.
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-6 flex flex-col gap-4">
      <div>
        <h2 className="text-base font-semibold">Plan de cuidado</h2>
        <p className="text-sm text-neutral-500 mt-0.5">
          Recomendaciones basadas en tu perfil y tu historial.
        </p>
      </div>

      <div className="flex flex-col gap-3">
        {plan.detailed.map((rec) => (
          <div key={rec.condition} className="rounded-lg border border-neutral-100 p-4 flex flex-col gap-1.5">
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm font-semibold capitalize">{rec.condition}</span>
              <span
                className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                  PRIORITY_COLORS[rec.priority] ?? PRIORITY_COLORS.MEDIA
                }`}
              >
                {rec.priority}
              </span>
            </div>
            <p className="text-sm text-neutral-600">{rec.advice}</p>
          </div>
        ))}
      </div>

      {plan.sourceConditions.length > 0 && (
        <p className="text-xs text-neutral-400">
          Basado en: {plan.sourceConditions.join(", ")}
        </p>
      )}
    </div>
  );
}
