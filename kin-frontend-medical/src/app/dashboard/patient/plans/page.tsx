"use client";

import { useEffect, useState } from "react";
import {
  patientPlansService,
  type PatientPlan,
} from "@/services/patientPlans";
import { subscriptionApi } from "@/services/subscriptionApi";

export default function PatientPlansPage() {
  const [plans, setPlans] = useState<PatientPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [banner, setBanner] = useState<"success" | "canceled" | null>(null);
  const [cancelling, setCancelling] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("success") === "true") setBanner("success");
    else if (params.get("canceled") === "true") setBanner("canceled");

    let cancelled = false;
    patientPlansService
      .getPatientPlans()
      .then((data) => {
        if (!cancelled) setPlans(data);
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

  const handleSubscribe = async (planId: string) => {
    setError("");
    try {
      const session = await patientPlansService.createCheckoutSession(planId);
      window.location.href = session.url;
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleCancelSubscription = async () => {
    if (!confirm("¿Estás seguro de que quieres cancelar tu suscripción? Perderás los beneficios del plan de pago y volverás al plan gratuito.")) {
      return;
    }
    setCancelling("cancel");
    setError("");
    try {
      const result = await subscriptionApi.cancelPatient();
      setBanner("canceled");
      setTimeout(() => window.location.reload(), 1500);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setCancelling(null);
    }
  };

  const paidPlan = plans.find(
    (p) => p.code === "PERSONAL_PLUS" || p.maxTriagesPerMonth === null
  );
  const freePlan = plans.find((p) => p.code === "FREE" || p.price === 0);

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Planes de triaje</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Elige el plan que mejor se adapte a tus consultas de triaje.
          </p>
        </div>

        {banner === "success" && (
          <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
            ¡Tu suscripción ha sido activada! Ya puedes realizar triajes
            ilimitados.
          </div>
        )}
        {banner === "canceled" && (
          <div className="rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3 text-sm text-yellow-800">
            El proceso de pago fue cancelado. Puedes intentarlo de nuevo cuando
            quieras.
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
            {plans.map((plan) => (
              <div
                key={plan.id}
                className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3"
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
                  {plan.maxTriagesPerMonth !== null
                    ? `${plan.maxTriagesPerMonth} triajes por mes`
                    : "Triajes ilimitados"}
                  {plan.trialDays != null &&
                    plan.trialDays > 0 &&
                    ` · ${plan.trialDays} días de prueba`}
                </p>

                <div className="mt-auto pt-2">
                  {plan.maxTriagesPerMonth !== null ? (
                    <>
                      <div className="rounded-lg bg-neutral-100 px-4 py-2 text-center text-xs font-medium text-neutral-600 mb-2">
                        Tu plan actual
                      </div>
                      <button
                        type="button"
                        onClick={handleCancelSubscription}
                        disabled={cancelling === "cancel"}
                        className="w-full rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-40"
                      >
                        {cancelling === "cancel" ? "Cancelando..." : "Cancelar suscripción"}
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleSubscribe(plan.id)}
                      disabled={paidPlan?.id !== plan.id}
                      className="w-full rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-40"
                    >
                      Contratar por ${plan.price}/mes
                    </button>
                  )}
                </div>
              </div>
            ))}
          </section>
        )}

        <p className="text-xs text-neutral-400">
          {freePlan && paidPlan
            ? `Compara tu plan gratuito (${freePlan.maxTriagesPerMonth ?? 0} triajes/mes) con ${paidPlan.name} ($${paidPlan.price}/mes) para triajes ilimitados.`
            : "Los pagos se procesan de forma segura a través de Stripe."}
        </p>
      </div>
    </main>
  );
}
