"use client";

import { useEffect, useState } from "react";
import { schedulingService } from "@/services/scheduling";
import type { PhysicianAvailability, SchedulingAppointment } from "@/services/scheduling";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function formatDate(iso: string | null | undefined) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    PENDIENTE: "bg-amber-100 text-amber-700",
    CONFIRMADA: "bg-emerald-100 text-emerald-700",
    CANCELADA: "bg-red-100 text-red-700",
    COMPLETADA: "bg-neutral-200 text-neutral-600",
    REPROGRAMADA: "bg-blue-100 text-blue-700",
  };
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${styles[status] ?? "bg-neutral-100 text-neutral-600"}`}>
      {status}
    </span>
  );
}

export default function PhysicianSchedulePage() {
  const [availability, setAvailability] = useState<PhysicianAvailability[]>([]);
  const [upcoming, setUpcoming] = useState<SchedulingAppointment[]>([]);
  const [history, setHistory] = useState<SchedulingAppointment[]>([]);
  const [error, setError] = useState("");

  // Formulario de disponibilidad
  const [day, setDay] = useState("MONDAY");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("17:00");
  const [slotDuration, setSlotDuration] = useState(30);

  const load = async () => {
    try {
      const [avail, up, hist] = await Promise.all([
        schedulingService.getAvailability(),
        schedulingService.upcomingAppointments(),
        schedulingService.appointmentHistory(),
      ]);
      setAvailability(avail);
      setUpcoming(up);
      setHistory(hist.filter((a) => a.status === "CANCELADA" || a.status === "COMPLETADA" || a.status === "REPROGRAMADA"));
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      schedulingService.getAvailability(),
      schedulingService.upcomingAppointments(),
      schedulingService.appointmentHistory(),
    ])
      .then(([avail, up, hist]) => {
        if (cancelled) return;
        setAvailability(avail);
        setUpcoming(up);
        setHistory(
          hist.filter((a) => a.status === "CANCELADA" || a.status === "COMPLETADA" || a.status === "REPROGRAMADA"),
        );
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSaveAvailability = async () => {
    setError("");
    try {
      await schedulingService.setAvailability({
        dayOfWeek: day,
        startTime: `${startTime}:00`,
        endTime: `${endTime}:00`,
        slotDurationMinutes: slotDuration,
        active: true,
      });
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleRemoveAvailability = async (id: string) => {
    setError("");
    try {
      await schedulingService.removeAvailability(id);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleConfirm = async (id: string) => {
    try {
      await schedulingService.confirmAppointment(id);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleCancel = async (id: string) => {
    try {
      await schedulingService.cancelAppointment(id, "Cancelada por el médico");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleComplete = async (id: string) => {
    try {
      await schedulingService.completeAppointment(id);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const [reschedulingId, setReschedulingId] = useState("");
  const [rescheduleDate, setRescheduleDate] = useState("");
  const [rescheduleTime, setRescheduleTime] = useState("");

  const handleReschedule = async (id: string) => {
    if (!rescheduleDate || !rescheduleTime) return;
    setError("");
    try {
      const newScheduledAt = new Date(`${rescheduleDate}T${rescheduleTime}:00`).toISOString();
      await schedulingService.rescheduleAppointment(id, newScheduledAt, "Reprogramada por el médico");
      setReschedulingId("");
      setRescheduleDate("");
      setRescheduleTime("");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Agenda</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Configura tu disponibilidad semanal y gestiona tus citas.
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

        <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
          <h2 className="text-base font-semibold">Disponibilidad</h2>
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
            <select
              value={day}
              onChange={(e) => setDay(e.target.value)}
              aria-label="Día de la semana"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {DAYS.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              aria-label="Hora de inicio"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              aria-label="Hora de fin"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="number"
              value={slotDuration}
              onChange={(e) => setSlotDuration(Number(e.target.value))}
              aria-label="Duración del slot (minutos)"
              min={15}
              step={15}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <button
              type="button"
              onClick={handleSaveAvailability}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition"
            >
              Guardar
            </button>
          </div>
          {availability.length > 0 && (
            <ul className="flex flex-wrap gap-2">
              {availability.map((a) => (
                <li
                  key={a.id}
                  className="flex items-center gap-2 rounded-lg border border-neutral-200 px-3 py-1.5 text-sm"
                >
                  <span className="font-medium">{a.dayOfWeek}</span>
                  <span className="text-neutral-500">
                    {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)} · {a.slotDurationMinutes} min
                  </span>
                  <button
                    type="button"
                    onClick={() => handleRemoveAvailability(a.id)}
                    aria-label={`Eliminar disponibilidad del ${a.dayOfWeek}`}
                    className="text-neutral-400 hover:text-red-600 text-lg leading-none"
                  >
                    ×
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-base font-semibold">Citas próximas</h2>
          {upcoming.length === 0 ? (
            <p className="text-sm text-neutral-500">Sin citas próximas.</p>
          ) : (
            <div className="flex flex-col gap-3">
              {upcoming.map((a) => (
                <div
                  key={a.id}
                  className="rounded-xl border border-neutral-200 bg-white p-4 flex flex-wrap items-center gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-neutral-800">{formatDate(a.scheduledAt)}</p>
                    <p className="text-xs text-neutral-500">{a.reason || "Cita"}</p>
                  </div>
                  <StatusBadge status={a.status} />
                  {a.status === "PENDIENTE" && (
                    <button
                      type="button"
                      onClick={() => handleConfirm(a.id)}
                      className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700 transition"
                    >
                      Confirmar
                    </button>
                  )}
                  {a.status === "CONFIRMADA" && (
                    <button
                      type="button"
                      onClick={() => setReschedulingId(reschedulingId === a.id ? "" : a.id)}
                      className="rounded-lg border border-blue-200 text-blue-700 px-3 py-1.5 text-xs font-medium hover:bg-blue-50 transition"
                    >
                      Reprogramar
                    </button>
                  )}
                  {reschedulingId === a.id && (
                    <div className="flex flex-wrap items-center gap-2">
                      <input
                        type="date"
                        value={rescheduleDate}
                        onChange={(e) => setRescheduleDate(e.target.value)}
                        aria-label="Nueva fecha"
                        className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                      />
                      <input
                        type="time"
                        value={rescheduleTime}
                        onChange={(e) => setRescheduleTime(e.target.value)}
                        aria-label="Nueva hora"
                        className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                      />
                      <button
                        type="button"
                        onClick={() => handleReschedule(a.id)}
                        disabled={!rescheduleDate || !rescheduleTime}
                        className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 transition disabled:bg-blue-300"
                      >
                        Guardar reprogramación
                      </button>
                    </div>
                  )}
                  {(a.status === "PENDIENTE" || a.status === "CONFIRMADA") && (
                    <>
                      <button
                        type="button"
                        onClick={() => handleCancel(a.id)}
                        className="rounded-lg border border-red-200 text-red-700 px-3 py-1.5 text-xs font-medium hover:bg-red-50 transition"
                      >
                        Cancelar
                      </button>
                      {a.status === "CONFIRMADA" && (
                        <button
                          type="button"
                          onClick={() => handleComplete(a.id)}
                          className="rounded-lg border border-primary-200 text-primary-700 px-3 py-1.5 text-xs font-medium hover:bg-primary-50 transition"
                        >
                          Completar
                        </button>
                      )}
                    </>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {history.length > 0 && (
          <section className="flex flex-col gap-3">
            <h2 className="text-base font-semibold">Historial</h2>
            <div className="flex flex-col gap-2">
              {history.slice(0, 10).map((a) => (
                <div key={a.id} className="flex items-center justify-between rounded-lg border border-neutral-100 px-4 py-2 text-sm">
                  <span className="text-neutral-700">{formatDate(a.scheduledAt)}</span>
                  <StatusBadge status={a.status} />
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
