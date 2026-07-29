"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { subscriptionApi } from "@/services/subscriptionApi";
import type { SubscriptionStatus, SubscriptionResponse } from "@/services/subscriptionApi";
import { authService } from "@/services/auth";

export default function SubscriptionPage() {
  const router = useRouter();
  const [status, setStatus] = useState<SubscriptionStatus | null>(null);
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [startingTrial, setStartingTrial] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [upgrades, setUpgrades] = useState<SubscriptionStatus["planName"][]>([]);

  useEffect(() => {
    const token = authService.getToken();
    if (!token) {
      router.push("/login");
      return;
    }

    Promise.all([
      subscriptionApi.getStatus(),
      subscriptionApi.getCurrent().catch(() => null),
    ])
      .then(([statusData, subData]) => {
        setStatus(statusData);
        setSubscription(subData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Error al cargar suscripción"))
      .finally(() => setLoading(false));
  }, [router]);

  const handleCancel = async () => {
    if (!confirm("¿Estás seguro de cancelar tu suscripción?")) return;
    setCancelling(true);
    setError(null);
    setSuccess(null);
    try {
      const updated = await subscriptionApi.cancel();
      setSubscription(updated);
      setStatus((prev) => prev ? { ...prev, isActive: false } : prev);
      setSuccess("Suscripción cancelada correctamente");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al cancelar");
    } finally {
      setCancelling(false);
    }
  };

  const handleStartTrial = async () => {
    setStartingTrial(true);
    setError(null);
    setSuccess(null);
    try {
      const result = await subscriptionApi.startTrial();
      setSubscription(result);
      setSuccess("¡Prueba gratuita iniciada! Disfruta de Premium Pro por 14 días");
      setTimeout(() => router.push("/dashboard/subscription"), 1000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al iniciar prueba");
    } finally {
      setStartingTrial(false);
    }
  };

  if (loading) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-500">Cargando suscripción...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-6 py-8 max-w-3xl mx-auto w-full">
      <h1 className="text-2xl font-bold tracking-tight mb-8">Mi Suscripción</h1>

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

      {status && (
        <>
          <div className="rounded-xl border border-neutral-200 bg-white p-6 shadow-sm mb-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold">Plan actual</h2>
              <span
                className={`text-xs px-3 py-1 rounded-full font-semibold ${
                  status.isActive
                    ? "bg-green-100 text-green-700"
                    : "bg-neutral-100 text-neutral-500"
                }`}
              >
                {status.isActive ? "Activo" : "Inactivo"}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-neutral-500">Plan</p>
                <p className="font-medium">{status.planName}</p>
              </div>
              <div>
                <p className="text-neutral-500">IA</p>
                <p className="font-medium">DeepSeek V4 {status.aiLevel}</p>
              </div>
              <div>
                <p className="text-neutral-500">Mensajes restantes</p>
                <p className="font-medium">
                  {status.messagesPerMonth === null
                    ? "Ilimitados"
                    : `${status.remainingMessages} / ${status.messagesPerMonth}`}
                </p>
              </div>
              <div>
                <p className="text-neutral-500">Proyectos</p>
                <p className="font-medium">
                  {status.maxProjects === null
                    ? "Ilimitados"
                    : `Máx. ${status.maxProjects}`}
                  {!status.canCreateProject && (
                    <span className="text-red-500 ml-1">(límite alcanzado)</span>
                  )}
                </p>
              </div>
              <div>
                <p className="text-neutral-500">Exportación PDF</p>
                <p className="font-medium">{status.pdfExport ? "Disponible" : "No disponible"}</p>
              </div>
              <div>
                <p className="text-neutral-500">Soporte</p>
                <p className="font-medium capitalize">{status.supportLevel.toLowerCase().replace("_", " ")}</p>
              </div>
            </div>

            {subscription && (
              <div className="mt-4 pt-4 border-t border-neutral-100 grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-neutral-500">Inicio</p>
                  <p className="font-medium">{new Date(subscription.startDate).toLocaleDateString()}</p>
                </div>
                {subscription.endDate && (
                  <div>
                    <p className="text-neutral-500">Finaliza</p>
                    <p className="font-medium">{new Date(subscription.endDate).toLocaleDateString()}</p>
                  </div>
                )}
                <div>
                  <p className="text-neutral-500">Estado</p>
                  <p className="font-medium capitalize">{subscription.status.toLowerCase()}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Mensajes usados</p>
                  <p className="font-medium">{subscription.messagesUsed}</p>
                </div>
              </div>
            )}
          </div>

          <div className="flex flex-wrap gap-3">
            {status.isActive && (
              <button
                onClick={handleCancel}
                disabled={cancelling}
                className="rounded-lg border border-red-200 bg-white px-5 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-11"
              >
                {cancelling ? "Cancelando..." : "Cancelar suscripción"}
              </button>
            )}

            {!status.isActive && !subscription && (
              <button
                onClick={handleStartTrial}
                disabled={startingTrial}
                className="rounded-lg bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-11"
              >
                {startingTrial ? "Iniciando..." : "Probar Premium Pro gratis 14 días"}
              </button>
            )}

            {!status.isActive && (
              <button
                onClick={() => router.push("/dashboard/pricing")}
                className="rounded-lg border border-neutral-200 bg-white px-5 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition min-h-11"
              >
                Ver planes disponibles
              </button>
            )}
          </div>
        </>
      )}
    </main>
  );
}
