"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { hasPhysicianCapability } from "@/utils/roles";
import { useNotificationCounts } from "@/hooks/useNotificationCounts";
import type { NotificationCounts } from "@/services/notifications";
import NotificationBadge from "@/components/layout/NotificationBadge";
import { useState } from "react";

interface NavItem {
  label: string;
  href: string;
}

const PATIENT_ITEMS: NavItem[] = [
  { label: "Mi Salud", href: "/dashboard/patient/health" },
  { label: "Triaje Digital", href: "/dashboard/patient/triage" },
  { label: "Planes", href: "/dashboard/patient/plans" },
  { label: "Invitaciones", href: "/dashboard/patient/invitations" },
  { label: "Mensajes", href: "/dashboard/patient/messages" },
  { label: "Citas", href: "/dashboard/patient/appointments" },
  { label: "Reservar cita", href: "/dashboard/patient/schedule" },
  { label: "Seguimiento", href: "/dashboard/patient/followup" },
  { label: "Documentos", href: "/dashboard/patient/documents" },
  { label: "Historial de accesos", href: "/dashboard/patient/audit" },
  { label: "Configuración", href: "/dashboard/settings" },
];

const PHYSICIAN_ITEMS: NavItem[] = [
  { label: "Portal Médico", href: "/dashboard/physician" },
  { label: "Planes", href: "/dashboard/physician/plans" },
  { label: "Mensajes", href: "/dashboard/physician/messages" },
  { label: "Agenda", href: "/dashboard/physician/schedule" },
  { label: "Citas", href: "/dashboard/physician/appointments" },
  { label: "Seguimiento", href: "/dashboard/physician/followup" },
  { label: "Documentos", href: "/dashboard/physician/documents" },
  { label: "Configuración", href: "/dashboard/physician/settings" },
];

export function countForNavItem(href: string, counts: NotificationCounts | null): number {
  if (!counts) return 0;
  switch (href) {
    case "/dashboard/patient/invitations":
      return counts.invitations;
    case "/dashboard/patient/messages":
    case "/dashboard/physician/messages":
      return counts.unreadMessages;
    case "/dashboard/patient/appointments":
      return counts.upcomingAppointments;
    case "/dashboard/physician/appointments":
      return counts.pendingAppointments;
    case "/dashboard/physician":
      return counts.highUrgencyAlerts;
    case "/dashboard/patient/followup":
      return counts.pendingTasks;
    case "/dashboard/physician/followup":
      return counts.overdueTasks;
    case "/dashboard/patient/schedule":
      return counts.upcomingAppointments;
    case "/dashboard/physician/schedule":
      return counts.pendingAppointments;
    default:
      return 0;
  }
}

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const asPhysician = hasPhysicianCapability(user);
  const items = asPhysician ? PHYSICIAN_ITEMS : PATIENT_ITEMS;
  const homeHref = asPhysician ? "/dashboard/physician" : "/dashboard/patient";
  const counts = useNotificationCounts(user?.role);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = async () => {
    await authService.logout();
    router.push("/login");
  };

  const isActive = (href: string) =>
    href === "/dashboard/physician" || href === "/dashboard/patient"
      ? pathname === href
      : pathname === href || pathname.startsWith(`${href}/`);

  const logo = (
    <div className="flex items-center gap-2">
      <div className="w-7 h-7 rounded-lg bg-medical-600 flex items-center justify-center shrink-0">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      </div>
      <span className="text-lg font-bold tracking-tight">KIN Medical</span>
    </div>
  );

  return (
    <>
      <aside className="hidden lg:flex lg:flex-col lg:w-60 xl:w-64 border-r border-neutral-200 bg-white shrink-0">
        <div className="px-6 pt-6 pb-4">
          <Link href={homeHref}>{logo}</Link>
        </div>

        <nav className="flex-1 px-3 space-y-1">
          {items.map((item) => {
            const active = isActive(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  active
                    ? "bg-medical-600 text-white"
                    : "text-neutral-600 hover:bg-medical-50 hover:text-medical-700"
                }`}
              >
                <span>{item.label}</span>
                <NotificationBadge count={countForNavItem(item.href, counts)} />
              </Link>
            );
          })}
        </nav>

        <div className="px-3 pb-6">
          <button
            onClick={handleLogout}
            className="w-full rounded-lg px-3 py-2 text-sm font-medium text-neutral-500 hover:bg-neutral-100 hover:text-red-600 transition text-left"
          >
            Cerrar sesión
          </button>
        </div>
      </aside>

      <nav className="lg:hidden flex items-center justify-between border-b border-neutral-200 bg-white px-4 py-3 shrink-0">
        <Link href={homeHref}>{logo}</Link>
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="flex items-center justify-center w-10 h-10 rounded-lg hover:bg-neutral-100 transition"
          aria-label={mobileMenuOpen ? "Cerrar menú" : "Abrir menú"}
        >
          {mobileMenuOpen ? (
            <svg className="w-5 h-5 text-neutral-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg className="w-5 h-5 text-neutral-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3 6h18M3 12h18M3 18h18" />
            </svg>
          )}
        </button>
      </nav>

      {mobileMenuOpen && (
        <div className="lg:hidden border-b border-neutral-200 bg-white px-4 py-3 space-y-1">
          {items.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              onClick={() => setMobileMenuOpen(false)}
              className={`block rounded-lg px-3 py-3 text-sm font-medium transition min-h-11 flex items-center justify-between gap-2 ${
                isActive(item.href)
                  ? "bg-medical-600 text-white"
                  : "text-neutral-600 hover:bg-medical-50 hover:text-medical-700"
              }`}
            >
              <span>{item.label}</span>
              <NotificationBadge count={countForNavItem(item.href, counts)} />
            </Link>
          ))}
          <button
            onClick={() => {
              setMobileMenuOpen(false);
              handleLogout();
            }}
            className="w-full rounded-lg px-3 py-3 text-sm font-medium text-neutral-500 hover:bg-neutral-100 hover:text-red-600 transition text-left min-h-11"
          >
            Cerrar sesión
          </button>
        </div>
      )}
    </>
  );
}