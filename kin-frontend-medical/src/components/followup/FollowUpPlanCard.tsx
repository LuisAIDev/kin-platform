"use client";

import { useState } from "react";
import type { FollowUpPlan } from "@/services/followup";

function formatDate(iso: string | null | undefined) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", { day: "numeric", month: "short", year: "numeric" });
}

function TaskStatusBadge({ status }: { status: string }) {
  if (status === "COMPLETED") {
    return <span className="rounded-full bg-emerald-100 text-emerald-700 px-2.5 py-0.5 text-xs font-bold">Completada</span>;
  }
  if (status === "OVERDUE") {
    return <span className="rounded-full bg-red-100 text-red-700 px-2.5 py-0.5 text-xs font-bold">Vencida</span>;
  }
  return <span className="rounded-full bg-amber-100 text-amber-700 px-2.5 py-0.5 text-xs font-bold">Pendiente</span>;
}

/**
 * Tarjeta de un plan de seguimiento con su lista de tareas.
 * - Modo "patient": botón para completar cada tarea pendiente/vencida.
 * - Modo "physician": formulario inline para añadir tareas.
 */
export default function FollowUpPlanCard({
  plan,
  mode,
  onCompleteTask,
  onAddTask,
}: {
  plan: FollowUpPlan;
  mode: "patient" | "physician";
  onCompleteTask?: (taskId: string) => void;
  onAddTask?: (planId: string, description: string, dueDate: string) => void;
}) {
  const [description, setDescription] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [adding, setAdding] = useState(false);

  const handleAdd = async () => {
    if (!onAddTask || !description.trim() || !dueDate) return;
    setAdding(true);
    try {
      await onAddTask(plan.planId, description.trim(), new Date(`${dueDate}T10:00:00`).toISOString());
      setDescription("");
      setDueDate("");
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="font-semibold text-neutral-800">{plan.title}</h3>
          {plan.description && <p className="text-sm text-neutral-500 mt-0.5">{plan.description}</p>}
          <p className="text-xs text-neutral-400 mt-1">
            Frecuencia: {plan.frequency} · Inicio: {formatDate(plan.startDate)}
            {plan.endDate ? ` · Fin: ${formatDate(plan.endDate)}` : ""}
          </p>
        </div>
        <span className="rounded-full bg-blue-100 text-blue-700 px-2.5 py-0.5 text-xs font-bold shrink-0">
          {plan.status}
        </span>
      </div>

      {plan.tasks.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {plan.tasks.map((task) => (
            <li
              key={task.taskId}
              className="flex items-center justify-between gap-2 rounded-lg border border-neutral-100 px-3 py-2"
            >
              <div className="flex flex-col gap-0.5 min-w-0">
                <span className="text-sm text-neutral-700">{task.description}</span>
                <span className="text-xs text-neutral-400">Vence: {formatDate(task.dueDate)}</span>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <TaskStatusBadge status={task.status} />
                {mode === "patient" &&
                  (task.status === "PENDING" || task.status === "OVERDUE") &&
                  onCompleteTask && (
                    <button
                      type="button"
                      onClick={() => onCompleteTask(task.taskId)}
                      className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 transition"
                    >
                      Completar
                    </button>
                  )}
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-neutral-400">Sin tareas.</p>
      )}

      {mode === "physician" && onAddTask && (
        <div className="border-t border-neutral-100 pt-3 flex flex-col gap-2">
          <p className="text-xs font-semibold uppercase text-neutral-400">Añadir tarea</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ej. Tomar medicación"
              aria-label={`Nueva tarea para ${plan.title}`}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              aria-label="Fecha de vencimiento"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <button
            type="button"
            onClick={handleAdd}
            disabled={adding || !description.trim() || !dueDate}
            className="self-start rounded-lg border border-primary-200 text-primary-700 px-3 py-1.5 text-xs font-medium hover:bg-primary-50 transition disabled:opacity-40"
          >
            {adding ? "Añadiendo..." : "Añadir tarea"}
          </button>
        </div>
      )}
    </div>
  );
}
