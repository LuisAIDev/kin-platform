"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { subscriptionApi } from "@/services/subscriptionApi";
import type { PricingPlan } from "@/services/pricing";
import { authService } from "@/services/auth";

export default function PricingPage() {
  const router = useRouter();
  const [plans, setPlans] = useState<PricingPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [subscribing, setSubscribing] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    const token = authService.getToken();
    if (!token) {
      router.push("/login");
      return;
    }

    subscriptionApi
      .getPlans()
      .then(setPlans)
      .catch((err) => setError(err instanceof Error ? err.message : "Error al cargar planes"))
      .finally(() => setLoading(false));
  }, [router]);

  const handleSubscribe = async (planId: string, price: number) => {
    setSubscribing(planId);
    setError(null);
    setSuccess(null);
    try {
      if (price === 0) {
        await subscriptionApi.subscribe(planId);
        setSuccess("Suscripción realizada con éxito");
        setTimeout(() => router.push("/dashboard/subscription"), 1500);
      } else {
        const origin = typeof window !== "undefined" ? window.location.origin : "http://localhost:3000";
        const checkout = await subscriptionApi.createCheckoutSession(
          planId,
          `${origin}/dashboard/subscription?success=true`,
          `${origin}/dashboard/pricing?cancelled=true`
        );
        window.location.href = checkout.url;
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al procesar la suscripción");
    } finally {
      setSubscribing(null);
    }
  };

  const isPaidPlan = (price: number) => price > 0;

  if (loading) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-500">Cargando planes...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight">Planes de Precios</h1>
        <p className="text-neutral-500 mt-1">Elige el plan que mejor se adapte a tus necesidades</p>
      </div>

      {error && (
        <div className="mb-6 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-6 rounded-lg bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
          {success}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        {plans.map((plan) => {
          const isPremium = plan.advancedAI;
          return (
            <div
              key={plan.id}
              className={
                isPremium
                  ? "relative rounded-2xl border-2 border-primary-500 bg-gradient-to-b from-white to-primary-50/40 p-8 shadow-lg shadow-primary-500/10"
                  : "relative rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm"
              }
            >
              {isPremium && (
                <span className="absolute -top-3 left-6 rounded-full bg-gradient-to-r from-primary-600 to-primary-500 px-4 py-1 text-xs font-semibold text-white shadow-sm">
                  Recomendado
                </span>
              )}

              <h3 className="text-xl font-semibold text-neutral-900">{plan.name}</h3>
              {plan.description && (
                <p className="mt-1 text-sm text-neutral-500">{plan.description}</p>
              )}

              <p className="mt-6">
                <span className="text-5xl font-bold tracking-tight text-neutral-900">
                  ${plan.price}
                </span>
                <span className="ml-1 text-sm text-neutral-400">/mes</span>
              </p>

              <ul className="mt-8 space-y-4" role="list">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-center gap-3 text-sm text-neutral-600">
                    <svg className="h-5 w-5 shrink-0 text-primary-500" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z" clipRule="evenodd" />
                    </svg>
                    {feature}
                  </li>
                ))}
              </ul>

              <button
                onClick={() => handleSubscribe(plan.id, plan.price)}
                disabled={subscribing === plan.id}
                className={
                  isPremium
                    ? "mt-8 flex w-full items-center justify-center rounded-xl bg-primary-600 px-6 py-3 text-sm font-semibold text-white shadow-md shadow-primary-600/20 hover:bg-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 min-h-11"
                    : "mt-8 flex w-full items-center justify-center rounded-xl border border-neutral-200 bg-white px-6 py-3 text-sm font-semibold text-neutral-700 shadow-sm hover:bg-neutral-50 hover:border-neutral-300 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 min-h-11"
                }
              >
                {subscribing === plan.id ? "Procesando..." : plan.price === 0 ? "Seleccionar plan gratuito" : "Suscribirse"}
              </button>
            </div>
          );
        })}
      </div>
    </main>
  );
}
