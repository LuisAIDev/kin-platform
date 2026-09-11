"use client";

import { useEffect, useState } from "react";
import FollowUpPlanCard from "@/components/followup/FollowUpPlanCard";
import EvolutionForm from "@/components/followup/EvolutionForm";
import EvolutionTimeline from "@/components/followup/EvolutionTimeline";
import { physicianService } from "@/services/physician";
import { followUpService } from "@/services/followup";
import type { FollowUpPlan, FollowUpTask, PatientEvolution } from "@/services/followup";

type Frequency = "DAILY" | "WEEKLY" | "MONTHLY";

export default function PhysicianFollowUpPage() {
  const [patients, setPatients] = useState<{ patientId: string; patientName: string }[]>([]);
  const [selected, setSelected] = useState<string>("");
  const [plans, setPlans] = useState<FollowUpPlan[]>([]);
  const [overdue, setOverdue] = useState<FollowUpTask[]>([]);
  const [evolution, setEvolution] = useState<PatientEvolution[]>([]);
  const [error, setError] = useState("");

  // Crear plan
  const [planTitle, setPlanTitle] = useState("");
  const [planDescription, setPlanDescription] = useState("");
  const [planFrequency, setPlanFrequency] = useState<Frequency>("WEEKLY");
  const [planStartDate, setPlanStartDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [creating, setCreating] = useState(false);

  const loadPlans = async (patientId: string) => {
    try {
      const data = await followUpService.plansForPatient(patientId);
      setPlans(data);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const loadEvolution = async (patientId: string) => {
    try {
      setEvolution(await followUpService.evolutionHistory(patientId));
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    physicianService
      .patients(0, 100, "ACTIVE")
      .then((page) => {
        if (cancelled) return;
        const list = page.content.map((p) => ({ patientId: p.patientId, patientName: p.patientName }));
        setPatients(list);
        if (list.length > 0) {
          setSelected(list[0].patientId);
        }
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    followUpService
      .overdueTasks()
      .then((data) => {
        if (!cancelled) setOverdue(data);
      })
      .catch(() => {
        // Sin acceso/errores transitorios: se omite.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selected) return;
    let cancelled = false;
    followUpService
      .plansForPatient(selected)
      .then((data) => {
        if (!cancelled) setPlans(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    followUpService
      .evolutionHistory(selected)
      .then((data) => {
        if (!cancelled) setEvolution(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, [selected]);

  const handleCreatePlan = async () => {
    if (!selected || !planTitle.trim()) return;
    setCreating(true);
    setError("");
    try {
      await followUpService.createPlan({
        patientId: selected,
        title: planTitle.trim(),
        description: planDescription.trim() || undefined,
        frequency: planFrequency,
        startDate: new Date(`${planStartDate}T10:00:00`).toISOString(),
      });
      setPlanTitle("");
      setPlanDescription("");
      await loadPlans(selected);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setCreating(false);
    }
  };

  const handleAddTask = async (planId: string, description: string, dueDate: string) => {
    await followUpService.addTask(planId, description, dueDate);
    await loadPlans(selected);
  };

  const selectedName = patients.find((p) => p.patientId === selected)?.patientName ?? "";

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Seguimiento de pacientes</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Planes de seguimiento, tareas y evolución de tus pacientes activos.
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

        {overdue.length > 0 && (
          <section className="rounded-xl border border-red-200 bg-red-50 p-4 flex flex-col gap-2">
            <h2 className="text-sm font-semibold text-red-800">Tareas vencidas ({overdue.length})</h2>
            <ul className="flex flex-col gap-1">
              {overdue.map((t) => (
                <li key={t.taskId} className="text-sm text-red-700">
                  • {t.description} (vence: {new Date(t.dueDate).toLocaleDateString("es-ES")})
                </li>
              ))}
            </ul>
          </section>
        )}

        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-semibold uppercase text-neutral-400">Selecciona un paciente</h2>
          <div className="flex flex-wrap gap-2">
            {patients.map((p) => (
              <button
                key={p.patientId}
                type="button"
                onClick={() => setSelected(p.patientId)}
                aria-pressed={selected === p.patientId}
                className={`rounded-lg border px-3 py-2 text-sm font-medium transition ${
                  selected === p.patientId
                    ? "bg-primary-600 text-white border-primary-600"
                    : "border-neutral-300 text-neutral-700 hover:bg-primary-50"
                }`}
              >
                {p.patientName}
              </button>
            ))}
          </div>
        </section>

        {selected && (
          <>
            <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
              <h2 className="text-base font-semibold">Nuevo plan para {selectedName}</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <input
                  type="text"
                  value={planTitle}
                  onChange={(e) => setPlanTitle(e.target.value)}
                  placeholder="Título del plan"
                  aria-label="Título del plan"
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <input
                  type="text"
                  value={planDescription}
                  onChange={(e) => setPlanDescription(e.target.value)}
                  placeholder="Descripción (opcional)"
                  aria-label="Descripción del plan"
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <select
                  value={planFrequency}
                  onChange={(e) => setPlanFrequency(e.target.value as Frequency)}
                  aria-label="Frecuencia"
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="DAILY">Diario</option>
                  <option value="WEEKLY">Semanal</option>
                  <option value="MONTHLY">Mensual</option>
                </select>
                <input
                  type="date"
                  value={planStartDate}
                  onChange={(e) => setPlanStartDate(e.target.value)}
                  aria-label="Fecha de inicio"
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
              <button
                type="button"
                onClick={handleCreatePlan}
                disabled={creating || !planTitle.trim()}
                className="self-start rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
              >
                {creating ? "Creando..." : "Crear plan"}
              </button>
            </section>

            <section className="flex flex-col gap-3">
              <h2 className="text-base font-semibold">Planes de {selectedName}</h2>
              {plans.length === 0 ? (
                <p className="text-sm text-neutral-500">Sin planes de seguimiento para este paciente.</p>
              ) : (
                <div className="grid grid-cols-1 gap-4">
                  {plans.map((plan) => (
                    <FollowUpPlanCard key={plan.planId} plan={plan} mode="physician" onAddTask={handleAddTask} />
                  ))}
                </div>
              )}
            </section>

            <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <EvolutionForm patientId={selected} onRecorded={() => loadEvolution(selected)} />
              <div className="flex flex-col gap-3">
                <h2 className="text-base font-semibold">Historial de evolución</h2>
                <EvolutionTimeline evolutions={evolution} />
              </div>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
