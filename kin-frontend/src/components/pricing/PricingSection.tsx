"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { pricingService } from "@/services/pricing";
import type { PricingPlan } from "@/services/pricing";

export default function PricingSection() {
  const [plans, setPlans] = useState<PricingPlan[]>([]);
  const [error, setError] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    pricingService
      .getAll()
      .then((data) => {
        setPlans(data);
        setLoaded(true);
      })
      .catch(() => {
        setError(true);
        setLoaded(true);
      });
  }, []);

  if (error) {
    return null;
  }

  return (
    <section id="precios" className="bg-gradient-to-b from-neutral-50 to-white py-24 sm:py-32">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-3">
            Planes
          </p>
          <h2 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
            Planes de Precios
          </h2>
          <p className="mt-4 text-lg text-neutral-500">
            Elige el plan que mejor se adapte a tu flujo de trabajo.
          </p>
        </div>

        {loaded && plans.length > 0 && (
          <div className="mx-auto mt-16 grid max-w-4xl grid-cols-1 gap-8 lg:grid-cols-2">
            {plans.map((plan) => {
              const isPremium = plan.isPopular;
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
                      Más popular
                    </span>
                  )}

                  <h3 className="text-lg font-semibold text-neutral-900">{plan.name}</h3>

                  <p className="mt-4">
                    <span className="text-5xl font-bold tracking-tight text-neutral-900">
                      {plan.currency === "USD" ? "$" : plan.currency}{plan.price}
                    </span>
                    <span className="ml-1 text-sm text-neutral-400">/{plan.billingPeriod === "monthly" ? "mes" : plan.billingPeriod}</span>
                  </p>

                  <ul className="mt-8 space-y-4" role="list">
                    {plan.features.map((feature) => (
                      <li key={feature} className="flex items-center gap-3 text-sm text-neutral-600">
                        <svg className="h-5 w-5 shrink-0 text-primary-500" viewBox="0 0 20 20" fill="currentColor">
                          <path
                            fillRule="evenodd"
                            d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                            clipRule="evenodd"
                          />
                        </svg>
                        {feature}
                      </li>
                    ))}
                  </ul>

                  <Link
                    href="/register"
                    className={
                      isPremium
                        ? "mt-8 flex w-full items-center justify-center rounded-xl bg-primary-600 px-6 py-3 text-sm font-semibold text-white shadow-md shadow-primary-600/20 hover:bg-primary-500 hover:shadow-lg hover:shadow-primary-600/30 transition-all duration-200"
                        : "mt-8 flex w-full items-center justify-center rounded-xl border border-neutral-200 bg-white px-6 py-3 text-sm font-semibold text-neutral-700 shadow-sm hover:bg-neutral-50 hover:border-neutral-300 transition-all duration-200"
                    }
                  >
                    {plan.price === 0 ? "Comenzar gratis" : "Ir a Premium"}
                  </Link>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
