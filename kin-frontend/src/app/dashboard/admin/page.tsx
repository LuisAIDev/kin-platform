"use client";

import Link from "next/link";
import { authService } from "@/services/auth";

interface AdminSection {
  title: string;
  description: string;
  links: { label: string; href: string }[];
}

const SECTIONS: AdminSection[] = [
  {
    title: "Vertical Empresa",
    description: "Estructuración de proyectos, analytics y reportes.",
    links: [
      { label: "Mis Proyectos", href: "/dashboard/projects" },
      { label: "Analytics", href: "/dashboard/analytics" },
      { label: "Insights", href: "/dashboard/insights" },
      { label: "Reportes", href: "/dashboard/reports" },
    ],
  },
  {
    title: "Vertical Salud",
    description: "Portal de salud para pacientes y médicos.",
    links: [
      { label: "Mi Salud", href: "/dashboard/patient/health" },
      { label: "Triaje Digital", href: "/dashboard/patient/triage" },
      { label: "Portal Médico", href: "/dashboard/physician" },
      { label: "Mensajes", href: "/dashboard/patient/messages" },
    ],
  },
  {
    title: "Administración",
    description: "Herramientas de administración de la plataforma.",
    links: [
      { label: "Planes de precios", href: "/dashboard/admin/pricing" },
      { label: "Verificación de médicos", href: "/dashboard/admin/physicians" },
      { label: "Dead Letter Queue", href: "/dashboard/admin/outbox/dead-letter" },
      { label: "Configuración", href: "/dashboard/settings" },
    ],
  },
];

export default function AdminHubPage() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">
          Administración
        </h1>
        <p className="text-sm text-neutral-500">
          Hola, {user?.fullName ?? "admin"}. Accede a todas las verticales de la plataforma.
        </p>
      </div>

      <section className="grid grid-cols-1 gap-4 mt-8 lg:grid-cols-3">
        {SECTIONS.map((section) => (
          <div key={section.title} className="rounded-xl border border-neutral-200 bg-white p-6">
            <h2 className="text-base font-bold text-neutral-900">{section.title}</h2>
            <p className="mt-1 text-xs text-neutral-500 leading-relaxed">{section.description}</p>
            <ul className="mt-4 space-y-2">
              {section.links.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="block rounded-lg px-3 py-2 text-sm font-medium text-neutral-600 hover:bg-primary-50 hover:text-primary-700 transition"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>
    </main>
  );
}
