"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  physicianPlansService,
  type PhysicianPlan,
} from "@/services/physicianPlans";

export default function PhysicianPlansPage() {
  const searchParams = useSearchParams();
  const highlightCode = searchParams.get('highlight');

  const [plans, setPlans] = useState<PhysicianPlan[]>([]);
  const [currentPlanCode, setCurrentPlanCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [banner, setBanner] = useState<"success" | "canceled" | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("success") === "true") setBanner("success");
    else if (params.get("canceled") === "true") setBanner("canceled");

    let cancelled = false;
    Promise.all([
      physicianPlansService.getPhysicianPlans(),
      // El endpoint /subscriptions/current devuelve la suscripción del usuario
      // autenticado; para un médico sin suscripción puede lanzar (no hay plan).
      // En ese caso se interpreta como "sin plan de pago" → plan gratuito.
      fetchCurrentPlanCode(),
    ])
      .then(([plansData, planCode]) => {
        if (cancelled) return;
        setPlans(plansData);
        setCurrentPlanCode(planCode);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const fetchCurrentPlanCode = async (): Promise<string | null> => {
    try {
      const res = await fetch("/api/v1/subscriptions/current", {
        credentials: "include",
      });
      if (!res.ok) return null;
      const body = await res.json();
      return body?.plan?.code ?? body?.planCode ?? null;
    } catch {
      return null;
    }
  };

  const handleSubscribe = async (planId: string) => {
    setError("");
    try {
      const session = await physicianPlansService.createCheckoutSession(planId);
      window.location.href = session.url;
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const paidPlan = plans.find((p) => p.price > 0);

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Planes Profesionales</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Accede a tu práctica clínica digital: invita pacientes, revisa sus
            triajes y gestiona tu cartera.
          </p>
        </div>

        {banner === "success" && (
          <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
            ¡Tu suscripción ha sido activada! Ya puedes invitar y gestionar
            pacientes.
          </div>
        )}
        {banner === "canceled" && (
          <div className="rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3 text-sm text-yellow-800">
            El proceso de pago fue cancelado. Puedes intentarlo de nuevo cuando
            quieras.
          </div>
        )}

        {highlightCode && (
          <div className="rounded-xl bg-medical-50 border border-medical-200 p-4 mb-4">
            <p className="text-sm text-medical-900">
              Continúa con tu registro: activa el plan <strong>{highlightCode}</strong> para comenzar.
            </p>
          </div>
        )}

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">
            {error}
          </p>
        )}

        {loading ? (
          <p className="text-sm text-neutral-500">Cargando planes...</p>
        ) : plans.length === 0 ? (
          <p className="text-sm text-neutral-500">
            No hay planes disponibles por el momento.
          </p>
        ) : (
          <section className="grid gap-4 md:grid-cols-2">
            {plans.map((plan) => {
              const isCurrent = currentPlanCode === plan.code;
              const isHighlighted = highlightCode === plan.code;
              return (
                <div
                  key={plan.id}
                  className={`rounded-xl border p-5 flex flex-col gap-3 transition ${
                    isCurrent
                      ? 'border-medical-500 bg-medical-50'
                      : isHighlighted
                      ? 'border-medical-500 ring-2 ring-medical-200 shadow-lg'
                      : 'border-neutral-200 bg-white'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h2 className="text-base font-semibold text-neutral-800">
                        {plan.name}
                      </h2>
                      <p className="text-sm text-neutral-500 mt-0.5">
                        {plan.description}
                      </p>
                    </div>
                    <span className="text-lg font-bold text-primary-700 shrink-0">
                      ${plan.price}
                      <span className="text-xs font-normal text-neutral-400">
                        /mes
                      </span>
                    </span>
                  </div>

                  <ul className="flex flex-col gap-1.5 text-sm text-neutral-600">
                    {plan.features.map((f, i) => (
                      <li key={i} className="flex gap-2">
                        <span className="text-primary-600">•</span>
                        {f}
                      </li>
                    ))}
                  </ul>

                  <p className="text-xs text-neutral-500">
                    {plan.trialDays != null && plan.trialDays > 0
                      ? `${plan.trialDays} días de prueba gratuita`
                      : plan.maxPatients != null
                        ? `Hasta ${plan.maxPatients} pacientes propios`
                        : "Pacientes ilimitados"}
                  </p>

                  <div className="mt-auto pt-2">
                    {isCurrent ? (
                      <div className="rounded-lg bg-neutral-100 px-4 py-2 text-center text-xs font-medium text-neutral-600">
                        Tu plan actual
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleSubscribe(plan.id)}
                        disabled={plan.price <= 0 || paidPlan?.id !== plan.id}
                        className="w-full rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-40 disabled:cursor-not-allowed"
                      >
                        {plan.price > 0
                          ? `Contratar por $${plan.price}/mes`
                          : "Sin costo inicial"}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </section>
        )}

        <p className="text-xs text-neutral-400">
          Los pagos se procesan de forma segura a través de Stripe. Al contratar
          el plan Profesional obtienes hasta 100 pacientes propios y acceso
          completo a triajes e historial.
        </p>
      </div>
    </main>
  );
}
