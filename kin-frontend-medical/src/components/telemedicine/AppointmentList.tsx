"use client";

import Link from "next/link";
import { Video } from "lucide-react";
import type { Appointment, AppointmentStatus } from "@/services/telemedicine";

const STATUS_COLORS: Record<AppointmentStatus, string> = {
  PENDIENTE: "bg-amber-100 text-amber-800",
  CONFIRMADA: "bg-emerald-100 text-emerald-800",
  CANCELADA: "bg-red-100 text-red-800",
  COMPLETADA: "bg-slate-100 text-slate-700",
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function AppointmentList({
  appointments,
  asPhysician,
  onStatusChange,
}: {
  appointments: Appointment[];
  asPhysician: boolean;
  onStatusChange?: (id: string, status: AppointmentStatus) => void;
}) {
  if (appointments.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-500">
        No hay citas.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {appointments.map((a) => (
        <div key={a.id} className="rounded-xl border border-neutral-200 bg-white p-4 flex items-start justify-between gap-3">
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[a.status]}`}>
                {a.status}
              </span>
              <span className="text-sm font-semibold text-neutral-800">{formatDate(a.scheduledAt)}</span>
            </div>
            <p className="text-sm text-neutral-600">{a.reason}</p>
          </div>

          {asPhysician && a.status === "PENDIENTE" && onStatusChange && (
            <div className="flex gap-2 shrink-0">
              <button
                type="button"
                onClick={() => onStatusChange(a.id, "CONFIRMADA")}
                className="rounded-lg bg-emerald-600 text-white px-3 py-1.5 text-xs font-medium hover:bg-emerald-700 transition"
              >
                Confirmar
              </button>
              <button
                type="button"
                onClick={() => onStatusChange(a.id, "CANCELADA")}
                className="rounded-lg border border-red-300 text-red-700 px-3 py-1.5 text-xs font-medium hover:bg-red-50 transition"
              >
                Rechazar
              </button>
            </div>
          )}

          {(['PENDIENTE', 'CONFIRMADA'] as string[]).includes(a.status) && (
            <Link
              href={`/dashboard/video/${a.id}`}
              className="inline-flex items-center gap-2 rounded-lg bg-medical-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-medical-700 transition shrink-0"
            >
              <Video className="w-4 h-4" />
              Videollamada
            </Link>
          )}
        </div>
      ))}
    </div>
  );
}
