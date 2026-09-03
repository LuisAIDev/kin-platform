"use client";

import { useEffect, useState } from "react";
import { PatientPlan, patientPlansService } from "@/services/patientPlans";
import { usePathname, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/dialog";
import { useTranslation } from "next-i18next";

export const metadata = {
  title: "Planes | KIN Health",
  description: "Selecciona tu plan de triaje: Free o Unlimited",
};

export default function PatientPlansPage() {
  const [plans, setPlans] = useState<PatientPlan[]>([]);
  const [currentPlan, setCurrentPlan] = useState<PatientPlan | null>(null);
  const [remainingTriages, setRemainingTriages] = useState<number | null>(null);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const hasSuccess = searchParams.get("success") === "true";
  const hasCanceled = searchParams.get("canceled") === "true";

  useEffect(() => {
    fetchPlans();
  }, [pathname]);

  const fetchPlans = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await patientPlansService.getPatientPlans();
      setPlans(response.plans);
      setCurrentPlan(response.currentPlan);
      setRemainingTriages(response.remainingTriages);
      setIsSubscribed(!!response.currentPlan);
    } catch (err) {
      setError((err as Error).message);
      console.error("Error fetching patient plans:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubscribe = async (planId: string) => {
    setError(null);
    try {
      const response = await patientPlansService.createCheckoutSession(planId);
      window.location.href = response.checkoutUrl;
    } catch (err) {
      setError((err as Error).message);
      console.error("Error creating checkout session:", err);
    }
  };

  return (
    <main className="min-h-screen bg-background">
      <div className="max-w-3xl mx-auto py-8 px-4">
        {error && (
          <div className="mb-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-800">
            <p className="text-sm">{error}</p>
          </div>
        )}

        {hasCanceled && (
          <div className="mb-6 p-4 rounded-lg bg-yellow-50 border border-yellow-200 text-yellow-800">
            <p className="text-sm">
              El proceso de pago fue cancelado. Puedes intentarlo de nuevo.
            </p>
          </div>
        )}

        {hasSuccess && (
          <div className="mb-6 p-4 rounded-lg bg-green-50 border border-green-200 text-green-800">
            <p className="text-sm">
              ¡Tu suscripción ha sido activada con éxito! Ya puedes disfrutar de triajes ilimitados.
            </p>
          </div>
        )}

        <Card>
          <CardHeader>
            <CardTitle>{currentPlan ? currentPlan.name : "Planes de Triaje"}</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <p className="text-sm">Cargando planes...</p>
            ) : plans.length === 0 ? (
              <p className="text-sm">No hay planes disponibles.</p>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {plans.map((plan) => (
                  <Card key={plan.id} className="p-4">
                    <CardHeader>
                      <CardTitle>{plan.name}</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <p className="text-sm text-neutral-600 line-clamp-3">
                        {plan.description}
                      </p>

                      <div className="mt-4 space-y-2">
                        <p className="text-xs text-neutral-500">
                          <strong>Precio:</strong> {plan.price} {plan.currency}/mes
                        </p>

                        <p className="text-xs text-neutral-500">
                          <strong>Triajes por mes:</strong>{" "}
                          {plan.maxTriagesPerMonth !== null
                            ? plan.maxTriagesPerMonth +
                                (plan.trialDays !== null
                                  ? ` (trial de ${plan.trialDays} días)`
                                  : "")
                            : "Ilimitados"}
                        </p>

                        {plan.viabilityScoringDetail === "BASIC" && (
                          <p className="text-xs text-neutral-500">
                            <strong>Scoring de viabilidad:</strong> BASIC
                          </p>
                        )}

                        {plan.triageSharing && (
                          <p className="text-xs text-neutral-500">
                            <strong>Compartir triajes:</strong> Sí
                          </p>
                        )}

                        {!plan.isPopular && (
                          <p className="text-xs text-neutral-500">
                            <strong>Popular:</strong> No
                          </p>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {currentPlan ? null : (
          <div className="mt-6">
            <h3 className="text-sm font-medium text-neutral-600 mb-3">
              Tu plan actual
            </h3>
            {currentPlan ? (
              <div className="p-4 rounded-lg bg-blue-50 border border-blue-200">
                <p className="text-sm text-blue-800">
                  <strong>{currentPlan.name}</strong> - {currentPlan.price} {currentPlan.currency}/mes
                </p>
                <p className="text-xs text-neutral-500">
                  Triajes {'%remainingTriages' !== undefined
                    ? `${remainingTriages}/${currentPlan.maxTriagesPerMonth} utilizados`
                    : "Ilimitados"}
                </p>
              </div>
            ) : (
              <p className="text-sm text-neutral-500">
                No tienes un plan activo. Contrata un plan para continuar.
              </p>
            )}
          </div>
        )}

        {currentPlan && currentPlan.code === "FREE" ? (
          <div className="mt-6 p-4 rounded-lg bg-yellow-50 border border-yellow-200">
            <p className="text-sm text-yellow-800">
              Has alcanzado el límite de triajes gratuitos (3 por mes).
              <br />
              <strong className="font-medium">Contrata el plan Unlimited por $9/mes</strong> para continuar con triajes ilimitados.
            </p>
          </div>
        ) : null}

        {!currentPlan || currentPlan.code !== "FREE" ? (
          <div className="mt-6">
            <h3 className="text-sm font-medium text-neutral-600 mb-3">
              Disponible para contratar
            </h3>
            {plans.some((p) => p.code === "PERSONAL_PLUS") && (
              <Button
                onClick={() => handleSubscribe(plans.find((p) => p.code === "PERSONAL_PLUS")!.id)}
                disabled={isSubscribed}
                className="w-full"
              >
                {isSubscribed ? "Suscrito" : "Contratar Unlimited por $9/mes"}
              </Button>
            )}
          </div>
        ) : null}
      </div>
    </main>
  );
}