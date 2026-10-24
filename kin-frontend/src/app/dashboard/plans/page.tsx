"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { subscriptionApi, type SubscriptionStatus } from "@/services/subscriptionApi";
import type { PricingPlan } from "@/services/pricing";

type Banner = { type: "success" | "cancelled" | "error"; message: string } | null;
type PaymentGateway = "WOMPI" | "STRIPE";

const SUPPORT_LABEL: Record<PricingPlan["supportLevel"], string> = {
  BASIC: "Soporte estándar",
  PREMIUM: "Soporte prioritario",
  SUPPORT_24_7: "Soporte 24/7",
};

function CheckIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="m4.5 10.5 3.5 3.5 7.5-8" />
    </svg>
  );
}

function ShieldIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M12 3 5 6v5c0 4.4 3 8.2 7 9.5 4-1.3 7-5.1 7-9.5V6l-7-3Z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}

function formatNumber(value: number): string {
  return value.toLocaleString("es-CO");
}

function planBadge(plan: PricingPlan): string | null {
  if (plan.code === "STANDARD") return "Recomendado";
  if (plan.code === "PREMIUM") return "Máximo rendimiento";
  return null;
}

function planButtonLabel(
  plan: PricingPlan,
  isCurrent: boolean,
  isActiveSubscription: boolean,
  isLocked: boolean,
  showCop: boolean,
  copAvailable: boolean,
): string {
  if (isCurrent && (isActiveSubscription || plan.price === 0)) return "Plan actual";
  if (isLocked) return "Plan activo en curso";
  if (plan.price === 0) return "Comenzar gratis";
  if (showCop && !copAvailable) return "No disponible en COP";
  return `Elegir ${plan.name}`;
}

export default function PlansPage() {
  const router = useRouter();
  const [plans, setPlans] = useState<PricingPlan[]>([]);
  const [status, setStatus] = useState<SubscriptionStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [subscribing, setSubscribing] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [banner, setBanner] = useState<Banner>(null);
  const [gateway, setGateway] = useState<PaymentGateway>("WOMPI");
  const gatewaySelectedManually = useRef(false);

  // Igual que en KIN Medical: si el navegador está en español se propone
  // Wompi (COP); en otro idioma, Stripe (USD). La elección manual manda.
  useEffect(() => {
    if (gatewaySelectedManually.current) return;
    const lang =
      typeof navigator !== "undefined" && navigator.language
        ? navigator.language.toLowerCase()
        : "es-co";
    setGateway(lang.startsWith("es") ? "WOMPI" : "STRIPE");
  }, []);

  const refreshStatus = useCallback(async () => {
    const nextStatus = await subscriptionApi.getStatus();
    setStatus(nextStatus);
    return nextStatus;
  }, []);

  useEffect(() => {
    let cancelled = false;

    Promise.all([subscriptionApi.getPlans(), subscriptionApi.getStatus()])
      .then(([plansData, statusData]) => {
        if (cancelled) return;

        const params = new URLSearchParams(window.location.search);
        if (params.get("success") === "true") {
          setBanner({
            type: "success",
            message: "Pago completado. Estamos activando tu plan; puede tardar unos segundos.",
          });
        } else if (params.get("cancelled") === "true") {
          setBanner({
            type: "cancelled",
            message: "El proceso de pago fue cancelado. Puedes intentarlo de nuevo cuando quieras.",
          });
        }

        setPlans(plansData);
        setStatus(statusData);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof Error && err.message.includes("401")) {
          router.push("/login");
          return;
        }
        setBanner({
          type: "error",
          message: err instanceof Error ? err.message : "No pudimos cargar los planes.",
        });
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [router]);

  const sortedPlans = useMemo(
    () => [...plans].sort((a, b) => a.price - b.price),
    [plans],
  );

  const currentPlanCode = status?.planCode ?? null;
  const hasActivePaidPlan =
    Boolean(status?.isActive) && currentPlanCode !== null && currentPlanCode !== "FREE";

  const handleSubscribe = async (plan: PricingPlan) => {
    setBanner(null);
    setSubscribing(plan.id);

    try {
      if (plan.price === 0) {
        await subscriptionApi.subscribe(plan.id);
        await refreshStatus();
        setBanner({
          type: "success",
          message: "Plan Gratuito activado. Ya puedes crear proyectos con los límites incluidos.",
        });
        return;
      }

      const copAvailable = plan.priceCop != null && plan.priceCop > 0;
      if (gateway === "WOMPI" && !copAvailable) {
        setBanner({
          type: "error",
          message: "Este plan no tiene precio en COP configurado. Elige pago internacional.",
        });
        return;
      }

      const origin = window.location.origin;
      const successUrl = `${origin}/dashboard/plans?success=true`;
      const cancelUrl = `${origin}/dashboard/plans?cancelled=true`;
      const checkout =
        gateway === "WOMPI"
          ? await subscriptionApi.createWompiCheckoutSession(plan.id, successUrl, cancelUrl)
          : await subscriptionApi.createCheckoutSession(plan.id, successUrl, cancelUrl);
      window.location.assign(checkout.url);
    } catch (err) {
      setBanner({
        type: "error",
        message: err instanceof Error ? err.message : "No pudimos procesar la suscripción.",
      });
    } finally {
      setSubscribing(null);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm("¿Seguro que quieres cancelar tu suscripción? Volverás al plan Gratuito.")) {
      return;
    }

    setBanner(null);
    setCancelling(true);

    try {
      await subscriptionApi.cancel();
      await refreshStatus();
      setBanner({
        type: "success",
        message: "Suscripción cancelada. Tu cuenta volvió al plan Gratuito.",
      });
    } catch (err) {
      setBanner({
        type: "error",
        message: err instanceof Error ? err.message : "No pudimos cancelar la suscripción.",
      });
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <main className="flex-1 bg-neutral-50/70">
        <div className="mx-auto w-full max-w-6xl px-5 py-10 sm:px-8 lg:py-14">
          <div className="h-4 w-40 animate-pulse rounded-full bg-neutral-200" />
          <div className="mt-4 h-10 w-full max-w-xl animate-pulse rounded-xl bg-neutral-200" />
          <div className="mt-10 grid gap-6 md:grid-cols-3">
            {[0, 1, 2].map((item) => (
              <div key={item} className="h-96 animate-pulse rounded-3xl border border-neutral-200 bg-white" />
            ))}
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 bg-neutral-50/70">
      <div className="mx-auto w-full max-w-6xl px-5 py-10 sm:px-8 lg:py-14">
        <header className="max-w-3xl">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-primary-600">
            Suscripción KIN
          </p>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-neutral-950 sm:text-4xl">
            Elige el plan que impulse tus proyectos
          </h1>
          <p className="mt-4 text-base leading-7 text-neutral-600">
            Planes mensuales en COP con Wompi o en USD con Stripe. Elige cómo prefieres pagar:
            el plan y sus beneficios son los mismos. Cambia de plan cuando quieras, sin permanencia.
          </p>
          <div className="mt-5 flex flex-wrap gap-2 text-xs font-medium text-neutral-600">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-neutral-200 bg-white px-3 py-1.5">
              <ShieldIcon className="h-4 w-4 text-primary-600" />
              Pago seguro con Wompi o Stripe
            </span>
            <span className="rounded-full border border-neutral-200 bg-white px-3 py-1.5">
              Facturación mensual
            </span>
            <span className="rounded-full border border-neutral-200 bg-white px-3 py-1.5">
              Sin permanencia
            </span>
          </div>
        </header>

        {banner && (
          <div
            role="status"
            aria-live="polite"
            className={`mt-8 rounded-2xl border px-4 py-3 text-sm font-medium ${
              banner.type === "error"
                ? "border-red-200 bg-red-50 text-red-700"
                : banner.type === "cancelled"
                  ? "border-amber-200 bg-amber-50 text-amber-800"
                  : "border-emerald-200 bg-emerald-50 text-emerald-800"
            }`}
          >
            {banner.message}
          </div>
        )}

        <div className="mt-8 rounded-lg border border-blue-200 bg-blue-50 p-3">
          <p className="text-sm text-blue-900">
            <strong>Elige cómo prefieres pagar.</strong> El plan y sus beneficios son los mismos en
            ambas opciones. Solo cambia la moneda.
          </p>
        </div>

        <div className="mt-6 mb-6">
          <label className="mb-2 block text-sm font-medium text-neutral-700">Método de pago</label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              aria-pressed={gateway === "WOMPI"}
              onClick={() => {
                gatewaySelectedManually.current = true;
                setGateway("WOMPI");
              }}
              className={`rounded-lg border-2 p-3 transition ${
                gateway === "WOMPI"
                  ? "border-primary-500 bg-primary-50"
                  : "border-neutral-200 hover:border-neutral-300"
              }`}
            >
              <p className="text-sm font-semibold">Colombia</p>
              <p className="text-xs text-neutral-500">PSE, Nequi, Tarjetas (COP)</p>
            </button>
            <button
              type="button"
              aria-pressed={gateway === "STRIPE"}
              onClick={() => {
                gatewaySelectedManually.current = true;
                setGateway("STRIPE");
              }}
              className={`rounded-lg border-2 p-3 transition ${
                gateway === "STRIPE"
                  ? "border-primary-500 bg-primary-50"
                  : "border-neutral-200 hover:border-neutral-300"
              }`}
            >
              <p className="text-sm font-semibold">Internacional</p>
              <p className="text-xs text-neutral-500">Tarjetas (USD)</p>
            </button>
          </div>
        </div>

        {status && (
          <section className="mt-8 overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm">
            <div className="flex flex-col gap-5 p-6 sm:p-7 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-lg font-semibold text-neutral-950">
                    {status.planName}
                  </h2>
                  <span
                    className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                      status.isActive
                        ? "bg-emerald-100 text-emerald-700"
                        : "bg-neutral-100 text-neutral-600"
                    }`}
                  >
                    {status.isActive ? "Activo" : "Plan base"}
                  </span>
                </div>
                <p className="mt-1 max-w-xl text-sm text-neutral-500">
                  {status.planDescription}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-x-8 gap-y-4 text-sm sm:grid-cols-4 lg:min-w-[34rem]">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">
                    Proyectos
                  </p>
                  <p className="mt-1 font-semibold text-neutral-900">
                    {status.maxProjects === null
                      ? "Ilimitados"
                      : `${status.completedProjects} / ${status.completedProjectsLimit}`}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">
                    Mensajes IA
                  </p>
                  <p className="mt-1 font-semibold text-neutral-900">
                    {status.messagesPerMonth === null
                      ? "Ilimitados"
                      : `${formatNumber(status.remainingMessages)} restantes`}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">
                    Nivel de IA
                  </p>
                  <p className="mt-1 font-semibold text-neutral-900">
                    {status.aiLevel === "PRO" ? "Pro" : "Flash"}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">
                    Soporte
                  </p>
                  <p className="mt-1 font-semibold text-neutral-900">
                    {SUPPORT_LABEL[status.supportLevel as PricingPlan["supportLevel"]] ??
                      status.supportLevel}
                  </p>
                </div>
              </div>
            </div>

            {hasActivePaidPlan && (
              <div className="flex flex-col gap-3 border-t border-neutral-100 bg-neutral-50/70 px-6 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-7">
                <p className="text-sm text-neutral-600">
                  ¿Quieres cambiar de plan? Cancela primero tu suscripción actual.
                </p>
                <button
                  type="button"
                  onClick={handleCancel}
                  disabled={cancelling}
                  className="inline-flex min-h-11 items-center justify-center rounded-xl border border-red-200 bg-white px-4 py-2 text-sm font-semibold text-red-600 transition hover:bg-red-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-400 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {cancelling ? "Cancelando..." : "Cancelar suscripción"}
                </button>
              </div>
            )}
          </section>
        )}

        <section className="mt-8 grid items-stretch gap-6 md:grid-cols-3" aria-label="Planes disponibles">
          {sortedPlans.map((plan) => {
            const isCurrent = plan.code !== null && plan.code === currentPlanCode;
            const isRecommended = plan.code === "STANDARD";
            const isLocked = hasActivePaidPlan && !isCurrent;
            const badge = planBadge(plan);
            const isBusy = subscribing === plan.id;
            const showCop = gateway === "WOMPI";
            const copAvailable = plan.priceCop != null && plan.priceCop > 0;
            const displayPrice =
              plan.price === 0 ? 0 : showCop ? (copAvailable ? plan.priceCop! : null) : plan.price;
            const disabled =
              isBusy ||
              (isCurrent && (Boolean(status?.isActive) || plan.price === 0)) ||
              isLocked ||
              (plan.price > 0 && showCop && !copAvailable);

            return (
              <article
                key={plan.id}
                className={`relative flex flex-col rounded-3xl border bg-white p-6 shadow-sm transition ${
                  isCurrent
                    ? "border-emerald-300 ring-2 ring-emerald-500/20"
                    : isRecommended
                      ? "border-primary-300 ring-2 ring-primary-500/15"
                      : "border-neutral-200"
                }`}
              >
                {badge && (
                  <span
                    className={`absolute -top-3 left-6 rounded-full px-3 py-1 text-xs font-semibold text-white shadow-sm ${
                      isRecommended
                        ? "bg-primary-600"
                        : "bg-neutral-900"
                    }`}
                  >
                    {badge}
                  </span>
                )}

                {isCurrent && (
                  <span className="absolute -top-3 right-6 rounded-full bg-emerald-600 px-3 py-1 text-xs font-semibold text-white shadow-sm">
                    Tu plan
                  </span>
                )}

                <div className="pt-2">
                  <h2 className="text-xl font-semibold text-neutral-950">{plan.name}</h2>
                  <p className="mt-2 min-h-10 text-sm leading-5 text-neutral-500">
                    {plan.description}
                  </p>
                </div>

                <div className="mt-6 flex items-end gap-1.5">
                  <span className="text-4xl font-bold tracking-tight text-neutral-950">
                    {displayPrice === null
                      ? "—"
                      : `$${showCop ? formatNumber(displayPrice) : displayPrice}`}
                  </span>
                  <span className="pb-1 text-sm text-neutral-500">
                    {plan.price === 0 ? "para siempre" : `${showCop ? "COP" : "USD"}/mes`}
                  </span>
                </div>

                {plan.price > 0 && (
                  <p className="mt-1 text-xs text-neutral-400">
                    {showCop
                      ? `También en USD: $${plan.price}/mes`
                      : copAvailable
                        ? `También en COP: $${formatNumber(plan.priceCop!)}/mes`
                        : "Pago internacional en USD"}
                  </p>
                )}

                {plan.trialDays ? (
                  <p className="mt-2 text-xs font-medium text-primary-700">
                    Incluye {plan.trialDays} días de prueba
                  </p>
                ) : (
                  <p className="mt-2 text-xs text-neutral-400">
                    Facturación mensual en {showCop ? "COP" : "USD"}
                  </p>
                )}

                <ul className="mt-6 space-y-3" role="list">
                  {plan.features.map((feature) => (
                    <li key={feature} className="flex gap-2.5 text-sm leading-6 text-neutral-700">
                      <CheckIcon className="mt-1 h-4 w-4 shrink-0 text-primary-600" />
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                <div className="mt-6 flex flex-wrap gap-2 text-xs font-medium text-neutral-600">
                  <span className="rounded-full bg-neutral-100 px-3 py-1.5">
                    {plan.maxProjects === null
                      ? "Proyectos ilimitados"
                      : `${plan.maxProjects} proyectos`}
                  </span>
                  <span className="rounded-full bg-neutral-100 px-3 py-1.5">
                    {plan.messagesPerMonth === null
                      ? "Mensajes ilimitados"
                      : `${formatNumber(plan.messagesPerMonth)} mensajes`}
                  </span>
                  <span className="rounded-full bg-neutral-100 px-3 py-1.5">
                    {plan.advancedAI ? "IA avanzada" : "IA incluida"}
                  </span>
                  {plan.pdfExport && (
                    <span className="rounded-full bg-neutral-100 px-3 py-1.5">
                      Exportación PDF
                    </span>
                  )}
                </div>

                <div className="mt-auto pt-7">
                  <button
                    type="button"
                    onClick={() => handleSubscribe(plan)}
                    disabled={disabled}
                    className={`flex min-h-11 w-full items-center justify-center rounded-xl px-4 py-2.5 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed ${
                      isCurrent
                        ? "border border-emerald-200 bg-emerald-50 text-emerald-700 focus-visible:ring-emerald-400"
                        : isRecommended
                          ? "bg-primary-600 text-white shadow-sm shadow-primary-600/20 hover:bg-primary-500 focus-visible:ring-primary-500"
                          : "border border-neutral-300 bg-white text-neutral-800 hover:bg-neutral-50 focus-visible:ring-neutral-400"
                    } ${disabled && !isCurrent ? "opacity-60" : ""}`}
                  >
                    {isBusy
                      ? "Procesando..."
                      : planButtonLabel(
                          plan,
                          isCurrent,
                          Boolean(status?.isActive),
                          isLocked,
                          showCop,
                          copAvailable,
                        )}
                  </button>
                  <p className="mt-3 text-center text-xs text-neutral-400">
                    {SUPPORT_LABEL[plan.supportLevel]}
                  </p>
                </div>
              </article>
            );
          })}
        </section>

        <p className="mt-8 text-center text-xs leading-5 text-neutral-400">
          Los pagos se procesan de forma segura con Wompi (PSE, Nequi y tarjetas en Colombia, COP)
          o con Stripe (tarjetas internacionales, USD). El plan y sus beneficios son los mismos.
        </p>
      </div>
    </main>
  );
}
