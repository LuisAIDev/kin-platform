"use client";

import { useEffect, useState } from "react";
import FollowUpPlanCard from "@/components/followup/FollowUpPlanCard";
import EvolutionTimeline from "@/components/followup/EvolutionTimeline";
import { followUpService } from "@/services/followup";
import type { FollowUpPlan, PatientEvolution } from "@/services/followup";

export default function PatientFollowUpPage() {
  const [plans, setPlans] = useState<FollowUpPlan[]>([]);
  const [evolution, setEvolution] = useState<PatientEvolution[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      const [plansData, evolutionData] = await Promise.all([
        followUpService.activePlans(),
        followUpService.ownEvolution(),
      ]);
      setPlans(plansData);
      setEvolution(evolutionData);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    Promise.all([followUpService.activePlans(), followUpService.ownEvolution()])
      .then(([plansData, evolutionData]) => {
        if (cancelled) return;
        setPlans(plansData);
        setEvolution(evolutionData);
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

  const handleCompleteTask = async (taskId: string) => {
    try {
      await followUpService.completeTask(taskId);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Seguimiento</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Tus planes de seguimiento activos y el historial registrado por tu médico.
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

        {loading ? (
          <p className="text-sm text-neutral-500">Cargando seguimiento...</p>
        ) : (
          <>
            <section className="flex flex-col gap-3">
              <h2 className="text-base font-semibold">Planes activos</h2>
              {plans.length === 0 ? (
                <div className="rounded-xl border border-dashed border-neutral-300 p-8 text-center text-sm text-neutral-500">
                  No tienes planes de seguimiento activos.
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4">
                  {plans.map((plan) => (
                    <FollowUpPlanCard
                      key={plan.planId}
                      plan={plan}
                      mode="patient"
                      onCompleteTask={handleCompleteTask}
                    />
                  ))}
                </div>
              )}
            </section>

            <section className="flex flex-col gap-3">
              <h2 className="text-base font-semibold">Mi evolución</h2>
              <EvolutionTimeline evolutions={evolution} />
            </section>
          </>
        )}
      </div>
    </main>
  );
}
