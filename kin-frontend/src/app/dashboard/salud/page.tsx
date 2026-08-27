"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { authService } from "@/services/auth";
import { isPhysicianRole } from "@/utils/roles";
import { dashboardService, type HealthSummary } from "@/services/dashboard";
import { telemedicineService, type Appointment } from "@/services/telemedicine";
import { physicianService, type ClinicalAlert } from "@/services/physician";

interface QuickAccess {
  label: string;
  href: string;
  description: string;
}

const PATIENT_QUICK_ACCESS: QuickAccess[] = [
  { label: "Mi Salud", href: "/dashboard/patient/health", description: "Resumen, historial y plan de cuidado" },
  { label: "Triaje Digital", href: "/dashboard/patient/triage", description: "Describe tus síntomas y obtén una sugerencia" },
  { label: "Mensajes", href: "/dashboard/patient/messages", description: "Conversa con tu médico asignado" },
  { label: "Citas", href: "/dashboard/patient/appointments", description: "Solicita y consulta citas de telemedicina" },
];

const PHYSICIAN_QUICK_ACCESS: QuickAccess[] = [
  { label: "Portal Médico", href: "/dashboard/physician", description: "Pacientes asignados y resúmenes clínicos" },
  { label: "Mensajes", href: "/dashboard/physician/messages", description: "Buzón de telemedicina" },
  { label: "Citas", href: "/dashboard/physician/appointments", description: "Confirma o reprograma citas" },
];

export default function SaludHubPage() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const role = user?.role;
  const isPhysician = isPhysicianRole(role);

  const [summary, setSummary] = useState<HealthSummary | null>(null);
  const [unread, setUnread] = useState(0);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [alerts, setAlerts] = useState<ClinicalAlert[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const requests: Promise<unknown>[] = [telemedicineService.appointments()];
    if (isPhysician) {
      requests.push(physicianService.alerts());
    } else {
      requests.push(dashboardService.summary());
      requests.push(telemedicineService.unread());
    }

    Promise.all(requests)
      .then((results) => {
        if (cancelled) return;
        const [appts] = results;
        setAppointments(appts as Appointment[]);
        if (isPhysician) {
          setAlerts((results[1] as ClinicalAlert[]) ?? []);
        } else {
          setSummary((results[1] as HealthSummary) ?? null);
          setUnread((results[2] as { unread: number } | undefined)?.unread ?? 0);
        }
      })
      .catch(() => {
        // Sin datos (errores transitorios): el hub muestra vacíos.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isPhysician]);

  const upcomingAppointments = appointments.filter((a) => a.status === "PENDIENTE" || a.status === "CONFIRMADA");
  const pendingAlerts = alerts.filter((a) => a.status === "PENDING" && a.severity === "ALTA").length;
  const quickAccess = isPhysician ? PHYSICIAN_QUICK_ACCESS : PATIENT_QUICK_ACCESS;

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">
          Hola, {user?.fullName ?? (isPhysician ? "médico" : "paciente")}
        </h1>
        <p className="text-sm text-neutral-500">
          {isPhysician
            ? "Portal de salud: pacientes, alertas, mensajería y citas."
            : "Tu espacio de salud: triaje, historial, mensajería y citas."}
        </p>
      </div>

      {/* KPIs */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3 mt-8">
        {isPhysician ? (
          <>
            <div className="rounded-xl border border-neutral-200 bg-white p-5">
              <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Alertas ALTA</p>
              <p className="mt-1 text-lg font-bold text-neutral-900">{pendingAlerts}</p>
              <p className="text-xs text-neutral-500 mt-0.5">Pendientes de revisión</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-5">
              <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Citas activas</p>
              <p className="mt-1 text-lg font-bold text-neutral-900">{upcomingAppointments.length}</p>
              <p className="text-xs text-neutral-500 mt-0.5">Pendientes o confirmadas</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-5">
              <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Rol</p>
              <p className="mt-1 text-lg font-bold text-neutral-900">Médico</p>
              <p className="text-xs text-neutral-500 mt-0.5">Portal médico asignado</p>
            </div>
          </>
        ) : (
          <>
            <div className="rounded-xl border border-neutral-200 bg-white p-5">
              <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Consultas</p>
              <p className="mt-1 text-lg font-bold text-neutral-900">{summary?.totalConsultations ?? 0}</p>
              <p className="text-xs text-neutral-500 mt-0.5">Triajes realizados</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-5">
              <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">No leídos</p>
              <p className="mt-1 text-lg font-bold text-neutral-900">{unread}</p>
              <p className="text-xs text-neutral-500 mt-0.5">Mensajes de tu médico</p>
            </div>
            <div className="rounded-xl border border-neutral-200 bg-white p-5">
              <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Último triaje</p>
              <p className="mt-1 text-lg font-bold text-neutral-900">
                {summary?.lastTriageAt ? new Date(summary.lastTriageAt).toLocaleDateString("es-ES") : "—"}
              </p>
              <p className="text-xs text-neutral-500 mt-0.5">Fecha del último triaje</p>
            </div>
          </>
        )}
      </section>

      {/* Accesos rápidos */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold mb-3">Accesos rápidos</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {quickAccess.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="group rounded-xl border border-neutral-200 bg-white p-5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
            >
              <h3 className="font-semibold text-sm text-emerald-700 group-hover:text-emerald-600">
                {item.label}
              </h3>
              <p className="text-xs text-neutral-500 mt-1 leading-relaxed">{item.description}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* Próximas citas */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold mb-3">Próximas citas</h2>
        {loading ? (
          <p className="text-sm text-neutral-500">Cargando citas...</p>
        ) : upcomingAppointments.length === 0 ? (
          <div className="rounded-xl border border-dashed border-neutral-300 p-8 text-center">
            <p className="text-sm text-neutral-500">No tienes citas próximas.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {upcomingAppointments.slice(0, 5).map((appt) => (
              <div key={appt.id} className="flex items-center justify-between rounded-xl border border-neutral-200 p-4">
                <div className="flex flex-col gap-0.5">
                  <p className="text-sm font-semibold text-neutral-800">
                    {new Date(appt.scheduledAt).toLocaleString("es-ES")}
                  </p>
                  <p className="text-xs text-neutral-500 line-clamp-1">{appt.reason}</p>
                </div>
                <span className="text-xs px-2 py-1 rounded-full bg-neutral-100 text-neutral-600 font-medium">
                  {appt.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
