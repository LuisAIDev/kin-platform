"use client";

import { useState } from "react";
import type { Project } from "@/services/projects";
import ExportProjectModal from "@/components/export/ExportProjectModal";

interface Props {
  project: Pick<Project, "id" | "title">;
}

/**
 * Botón "Exportar proyecto" (módulo kin.export). Abre el modal de exportación
 * con el proyecto real como fuente (Project + ConsultingReport + project_info),
 * independiente del botón legacy "Descargar Reporte PDF".
 */
export default function ExportProjectButton({ project }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-2 w-full rounded-xl border border-primary-600 px-5 py-2.5 text-sm font-medium text-primary-700 hover:bg-primary-50 transition mt-3"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="12" y1="18" x2="12" y2="12" />
          <polyline points="9 15 12 12 15 15" />
        </svg>
        Exportar proyecto
      </button>

      {open && (
        <ExportProjectModal
          projectId={project.id}
          projectTitle={project.title}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
