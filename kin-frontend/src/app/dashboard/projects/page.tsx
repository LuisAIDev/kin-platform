"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { projectsService, type Project } from "@/services/projects";
import { authService } from "@/services/auth";
import { forceLogout } from "@/services/session";

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = authService.getToken();
    if (!token) {
      router.push("/login");
      return;
    }

    projectsService
      .getAll()
      .then(setProjects)
      .catch(() => {
        forceLogout();
      })
      .finally(() => setLoading(false));
  }, [router]);

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
        <h1 className="text-2xl font-bold">Mis Proyectos</h1>
        <button
          onClick={() => router.push("/dashboard/projects/new")}
          className="rounded-lg bg-neutral-900 px-5 py-2 text-sm font-medium text-white hover:bg-neutral-800 transition"
        >
          Nuevo proyecto
        </button>
      </div>

      {projects.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-neutral-500 mb-4">
            Aún no tienes proyectos
          </p>
          <button
            onClick={() => router.push("/dashboard/projects/new")}
            className="rounded-lg bg-neutral-900 px-6 py-3 text-sm font-medium text-white hover:bg-neutral-800 transition"
          >
            Crear primer proyecto
          </button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <div
              key={project.id}
              onClick={() => router.push(`/dashboard/projects/${project.id}`)}
              className="rounded-xl border border-neutral-200 p-5 hover:shadow-md transition cursor-pointer"
            >
              <h2 className="font-semibold mb-1 truncate">{project.title}</h2>
              {project.description && (
                <p className="text-sm text-neutral-500 line-clamp-2">
                  {project.description}
                </p>
              )}
              <div className="flex gap-2 mt-3">
                <span className="text-xs bg-neutral-100 px-2 py-1 rounded-full">
                  {project.category}
                </span>
                <span className="text-xs bg-neutral-100 px-2 py-1 rounded-full">
                  {project.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
