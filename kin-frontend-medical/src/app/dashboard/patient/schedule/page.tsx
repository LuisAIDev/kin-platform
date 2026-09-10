"use client";

import { useEffect, useState } from "react";
import { schedulingService } from "@/services/scheduling";
import { telemedicineService } from "@/services/telemedicine";
import type { SchedulingAppointment } from "@/services/scheduling";
import type { Contact } from "@/services/telemedicine";

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

export default function PatientSchedulePage() {
  const [physicians, setPhysicians] = useState<Contact[]>([]);
  const [selectedPhysician, setSelectedPhysician] = useState("");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [slots, setSlots] = useState<string[]>([]);
  const [upcoming, setUpcoming] = useState<SchedulingAppointment[]>([]);
  const [history, setHistory] = useState<SchedulingAppointment[]>([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadAppointments = async () => {
    try {
      const [up, hist] = await Promise.all([
        schedulingService.upcomingAppointments(),
        schedulingService.appointmentHistory(),
      ]);
      setUpcoming(up);
      setHistory(hist);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    telemedicineService
      .contacts()
      .then((contacts) => {
        if (cancelled) return;
        setPhysicians(contacts);
        if (contacts.length > 0) setSelectedPhysician(contacts[0].id);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    Promise.all([
      schedulingService.upcomingAppointments(),
      schedulingService.appointmentHistory(),
    ])
      .then(([up, hist]) => {
        if (cancelled) return;
        setUpcoming(up);
        setHistory(hist);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedPhysician || !date) return;
    let cancelled = false;
    schedulingService
      .slotsForPhysician(selectedPhysician, date)
      .then((data) => {
        if (!cancelled) setSlots(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedPhysician, date]);

  const handleRequest = async (scheduledAt: string) => {
    setError("");
    setMessage("");
    try {
      await schedulingService.requestAppointment({
        physicianId: selectedPhysician,
        scheduledAt,
        durationMinutes: 30,
        reason: "Cita solicitada desde la agenda",
      });
      setMessage("Cita solicitada. Espera la confirmación del médico.");
      setSlots((prev) => prev.filter((s) => s !== scheduledAt));
      await loadAppointments();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleCancel = async (id: string) => {
    try {
      await schedulingService.cancelAppointment(id, "Cancelada por el paciente");
      await loadAppointments();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Reservar cita</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Elige a tu médico, una fecha y un horario disponible.
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}
        {message && <p className="text-sm text-emerald-700 bg-emerald-50 px-4 py-2.5 rounded-lg">{message}</p>}

        <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <select
              value={selectedPhysician}
              onChange={(e) => setSelectedPhysician(e.target.value)}
              aria-label="Médico"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {physicians.length === 0 && <option value="">Sin médicos asignados</option>}
              {physicians.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              aria-label="Fecha"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div className="flex flex-col gap-2">
            <p className="text-xs font-semibold uppercase text-neutral-400">Horarios disponibles</p>
            {slots.length === 0 ? (
              <p className="text-sm text-neutral-500">Sin horarios disponibles para esta fecha.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {slots.map((slot) => (
                  <button
                    key={slot}
                    type="button"
                    onClick={() => handleRequest(slot)}
                    className="rounded-lg border border-primary-200 text-primary-700 px-3 py-2 text-sm font-medium hover:bg-primary-50 transition"
                  >
                    {new Date(slot).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" })}
                  </button>
                ))}
              </div>
            )}
          </div>
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-base font-semibold">Mis citas próximas</h2>
          {upcoming.length === 0 ? (
            <p className="text-sm text-neutral-500">No tienes citas próximas.</p>
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
                  {(a.status === "PENDIENTE" || a.status === "CONFIRMADA") && (
                    <button
                      type="button"
                      onClick={() => handleCancel(a.id)}
                      className="rounded-lg border border-red-200 text-red-700 px-3 py-1.5 text-xs font-medium hover:bg-red-50 transition"
                    >
                      Cancelar
                    </button>
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
