"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { projectsService, type Project } from "@/services/projects";
import { subscriptionApi, type SubscriptionStatus } from "@/services/subscriptionApi";
import { authService } from "@/services/auth";
import { statusBadge } from "@/utils/badgeColors";

interface QuickAccess {
  label: string;
  href: string;
  description: string;
}

const QUICK_ACCESS: QuickAccess[] = [
  { label: "Nuevo proyecto", href: "/dashboard/projects/new", description: "Estructura una nueva idea con IA" },
  { label: "Analytics", href: "/dashboard/analytics", description: "Métricas de uso e insights del producto" },
  { label: "Insights", href: "/dashboard/insights", description: "Conclusiones inteligentes de tus sesiones" },
  { label: "Reportes", href: "/dashboard/reports", description: "Informes y exportaciones de tus proyectos" },
];

export default function EmpresaHubPage() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const [projects, setProjects] = useState<Project[]>([]);
  const [subscription, setSubscription] = useState<SubscriptionStatus | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    Promise.all([projectsService.getAll(0), subscriptionApi.getStatus()])
      .then(([res, sub]) => {
        if (cancelled) return;
        setProjects(res.content);
        setSubscription(sub);
      })
      .catch(() => {
        // Sin datos (errores transitorios): los hubs muestran vacíos.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const projectsUsed = subscription?.completedProjects ?? projects.length;
  const projectsLimit = subscription?.completedProjectsLimit;

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">
          Hola, {user?.fullName ?? "empresario"}
        </h1>
        <p className="text-sm text-neutral-500">
          Estructura, analiza y haz crecer tus proyectos con KIN.
        </p>
      </div>

      {/* KPIs */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3 mt-8">
        <div className="rounded-xl border border-neutral-200 bg-white p-5">
          <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Plan</p>
          <p className="mt-1 text-lg font-bold text-neutral-900">
            {subscription?.planName ?? "GRATIS"}
          </p>
          <p className="text-xs text-neutral-500 mt-0.5">
            {subscription ? "Suscripción activa" : "Plan gratuito"}
          </p>
        </div>
        <div className="rounded-xl border border-neutral-200 bg-white p-5">
          <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Proyectos</p>
          <p className="mt-1 text-lg font-bold text-neutral-900">
            {projectsUsed}
            {projectsLimit !== null ? ` / ${projectsLimit}` : " +"}
          </p>
          <p className="text-xs text-neutral-500 mt-0.5">Completados en el período</p>
        </div>
        <div className="rounded-xl border border-neutral-200 bg-white p-5">
          <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">Mensajes IA</p>
          <p className="mt-1 text-lg font-bold text-neutral-900">
            {subscription?.messagesPerMonth !== null && subscription?.messagesPerMonth !== undefined
              ? `${subscription.messagesPerMonth}`
              : "∞"}
          </p>
          <p className="text-xs text-neutral-500 mt-0.5">Cuota mensual de conversación</p>
        </div>
      </section>

      {/* Accesos rápidos */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold mb-3">Accesos rápidos</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {QUICK_ACCESS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="group rounded-xl border border-neutral-200 bg-white p-5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
            >
              <h3 className="font-semibold text-sm text-primary-700 group-hover:text-primary-600">
                {item.label}
              </h3>
              <p className="text-xs text-neutral-500 mt-1 leading-relaxed">{item.description}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* Proyectos recientes */}
      <section className="mt-8">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold">Proyectos recientes</h2>
          <Link href="/dashboard/projects" className="text-sm font-medium text-primary-600 hover:text-primary-700">
            Ver todos
          </Link>
        </div>

        {loading ? (
          <p className="text-sm text-neutral-500">Cargando proyectos...</p>
        ) : projects.length === 0 ? (
          <div className="rounded-xl border border-dashed border-neutral-300 p-8 text-center">
            <p className="text-sm text-neutral-500 mb-3">Aún no tienes proyectos.</p>
            <Link
              href="/dashboard/projects/new"
              className="inline-flex rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition"
            >
              Crear primer proyecto
            </Link>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {projects.slice(0, 6).map((project) => (
              <Link
                key={project.id}
                href={`/dashboard/projects/${project.id}`}
                className="block rounded-xl border border-neutral-200 p-5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              >
                <h3 className="font-semibold text-base mb-1 truncate">{project.title}</h3>
                {project.description && (
                  <p className="text-sm text-neutral-500 line-clamp-2 leading-relaxed">
                    {project.description}
                  </p>
                )}
                <div className="flex gap-2 mt-3">
                  <span
                    className="text-xs px-2 py-1 rounded-full font-medium"
                    style={{
                      backgroundColor: project.categoryColor ? `${project.categoryColor}1A` : undefined,
                      color: project.categoryColor ?? undefined,
                    }}
                  >
                    {project.categoryName ?? project.category}
                  </span>
                  <span className={`text-xs px-2 py-1 rounded-full font-medium ${statusBadge(project.status)}`}>
                    {project.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
