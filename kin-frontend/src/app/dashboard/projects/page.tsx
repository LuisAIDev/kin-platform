"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { projectsService, type Project } from "@/services/projects";
import { authService } from "@/services/auth";
import { forceLogout } from "@/services/session";
import ProgressCircle from "@/components/ProgressCircle";
import { categoryBadge, statusBadge } from "@/utils/badgeColors";
import ConfirmDialog from "@/components/ConfirmDialog";

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [deleteTarget, setDeleteTarget] = useState<Project | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    const token = authService.getToken();
    if (!token) {
      router.push("/login");
      return;
    }

    let cancelled = false;

    projectsService
      .getAll(page)
      .then((res) => {
        if (cancelled) return;
        setProjects(res.content);
        setTotalPages(res.totalPages);
      })
      .catch(() => {
        if (!cancelled) forceLogout();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [page, router]);

  if (loading) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-500">Cargando...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold tracking-tight">Mis Proyectos</h1>
        <button
          onClick={() => router.push("/dashboard/projects/new")}
          className="rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none min-h-11"
        >
          Nuevo proyecto
        </button>
      </div>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Eliminar proyecto"
        message={
          deleteTarget
            ? `¿Estás seguro de eliminar "${deleteTarget.title}"? Esta acción no se puede deshacer.`
            : ""
        }
        onConfirm={async () => {
          if (!deleteTarget) return;
          setDeleting(true);
          setDeleteError(null);
          try {
            await projectsService.delete(deleteTarget.id);
            setProjects((prev) => prev.filter((p) => p.id !== deleteTarget.id));
            setDeleteTarget(null);
          } catch (err) {
            setDeleteError(err instanceof Error ? err.message : "Error al eliminar el proyecto");
          } finally {
            setDeleting(false);
          }
        }}
        onCancel={() => {
          setDeleteTarget(null);
          setDeleteError(null);
        }}
        loading={deleting}
      />

      {projects.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-neutral-500 mb-4">
            Aún no tienes proyectos
          </p>
          <button
            onClick={() => router.push("/dashboard/projects/new")}
          className="rounded-lg bg-primary-600 px-6 py-3 text-sm font-medium text-white hover:bg-primary-700 transition focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none min-h-11"
        >
          Crear primer proyecto
          </button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <div
              key={project.id}
              className="rounded-xl border border-neutral-200 p-5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 relative group"
            >
              <Link
                href={`/dashboard/projects/${project.id}`}
                className="block focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none"
              >
              <h2 className="font-semibold text-base mb-1 truncate">{project.title}</h2>
              {project.description && (
                <p className="text-sm text-neutral-500 line-clamp-2 leading-relaxed">
                  {project.description}
                </p>
              )}
              <div className="flex gap-2 mt-3">
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${categoryBadge(project.category)}`}>
                  {project.category}
                </span>
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${statusBadge(project.status)}`}>
                  {project.status}
                </span>
              </div>
              {project.progressPercentage !== null && (
                <div className="mt-3 pt-3 border-t border-neutral-100">
                  <ProgressCircle percentage={project.progressPercentage} />
                </div>
              )}
              </Link>
              <button
                onClick={() => setDeleteTarget(project)}
                aria-label="Eliminar proyecto"
                className="absolute top-3 right-3 flex items-center justify-center w-7 h-7 rounded-full text-neutral-400 hover:text-red-600 hover:bg-red-50 transition md:opacity-0 md:group-hover:opacity-100 focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4" aria-hidden="true">
                  <path fillRule="evenodd" d="M8.75 1A2.75 2.75 0 006 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 10.23 1.482l.149-.022.841 10.518A2.75 2.75 0 007.596 19h4.807a2.75 2.75 0 002.742-2.53l.841-10.52.149.023a.75.75 0 00.23-1.482A41.03 41.03 0 0014 4.193V3.75A2.75 2.75 0 0011.25 1h-2.5zM10 4c.84 0 1.673.025 2.5.075V3.75c0-.69-.56-1.25-1.25-1.25h-2.5c-.69 0-1.25.56-1.25 1.25v.325C8.327 4.025 9.16 4 10 4zM8.58 7.72a.75.75 0 00-1.5.06l.3 7.5a.75.75 0 101.5-.06l-.3-7.5zm4.34.06a.75.75 0 10-1.5-.06l-.3 7.5a.75.75 0 101.5.06l.3-7.5z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-8">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-40 disabled:cursor-not-allowed min-h-11"
          >
            &larr; Anterior
          </button>
          <span className="text-sm text-neutral-500">
            Página {page + 1} de {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-40 disabled:cursor-not-allowed min-h-11"
          >
            Siguiente &rarr;
          </button>
        </div>
      )}

      {deleteError && (
        <div aria-live="polite" className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 rounded-lg bg-red-600 px-5 py-3 text-sm text-white shadow-lg">
          {deleteError}
          <button
            onClick={() => setDeleteError(null)}
            className="ml-3 font-bold hover:opacity-80"
          >
            ✕
          </button>
        </div>
      )}
    </main>
  );
}
