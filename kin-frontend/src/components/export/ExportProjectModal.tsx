"use client";

import { useEffect, useRef, useState } from "react";
import { downloadBlob } from "@/services/enterpriseApi";
import {
  exportProjectService,
  ExportApiError,
  type ExportFormat,
  type ExportOptions,
} from "@/services/exportProject";
import {
  projectInfoService,
  type ProjectDocumentItem,
} from "@/services/projectInfo";

type ExportOption = "complete" | "template" | "summary";

interface Props {
  projectId: string;
  projectTitle: string;
  onClose: () => void;
}

const FORMAT_LABELS: Record<ExportFormat, string> = {
  DOCX: "Word (.docx)",
  PDF: "PDF (.pdf)",
  MARKDOWN: "Markdown (.md)",
};

const FORMAT_EXT: Record<ExportFormat, string> = {
  DOCX: "docx",
  PDF: "pdf",
  MARKDOWN: "md",
};

function statusMessage(status: number, fallback: string): string {
  switch (status) {
    case 401:
      return "Tu sesión expiró. Inicia sesión nuevamente e inténtalo.";
    case 403:
      return "No tienes permisos para exportar este proyecto.";
    case 404:
      return "El proyecto no fue encontrado.";
    case 400:
      return fallback || "La solicitud no es válida.";
    default:
      return "Ocurrió un error inesperado. Inténtalo de nuevo.";
  }
}

export default function ExportProjectModal({ projectId, projectTitle, onClose }: Props) {
  const [option, setOption] = useState<ExportOption>("complete");
  const [format, setFormat] = useState<ExportFormat>("DOCX");
  const [documents, setDocuments] = useState<ProjectDocumentItem[]>([]);
  const [templateId, setTemplateId] = useState("");
  const [options, setOptions] = useState<ExportOptions | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [status, setStatus] = useState<{ type: "ok" | "error"; text: string } | null>(null);
  const [templateApplied, setTemplateApplied] = useState(false);
  const generatingRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      exportProjectService.options(projectId).catch(() => null),
      projectInfoService.listDocuments(projectId).catch(() => [] as ProjectDocumentItem[]),
    ]).then(([opts, docs]) => {
      if (cancelled) {
        return;
      }
      setOptions(opts);
      setDocuments((docs ?? []).filter((doc) => doc.status === "PROCESADO"));
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const canDownload = !generating;

  async function handleDownload() {
    if (generatingRef.current) {
      return;
    }
    if (option === "template" && !templateId) {
      setStatus({ type: "error", text: "Selecciona un documento de referencia." });
      return;
    }
    generatingRef.current = true;
    setGenerating(true);
    setStatus(null);
    setTemplateApplied(false);

    const mode = option === "summary" ? "SUMMARY" : "COMPLETE";
    const ext = FORMAT_EXT[format];
    const filename = `${options?.filenameBase ?? "KIN_proyecto"}.${ext}`;

    try {
      const blob =
        option === "template" && templateId
          ? await exportProjectService.download(projectId, format, mode, templateId)
          : await exportProjectService.download(projectId, format, mode);
      setStatus({ type: "ok", text: "Preparando descarga…" });
      downloadBlob(blob, filename);
      if (option === "template") {
        setTemplateApplied(true);
      }
      setStatus({ type: "ok", text: "✓ Proyecto descargado correctamente." });
    } catch (err) {
      const statusCode = err instanceof ExportApiError ? err.status : 500;
      const fallback = err instanceof Error ? err.message : "";
      setStatus({ type: "error", text: statusMessage(statusCode, fallback) });
    } finally {
      generatingRef.current = false;
      setGenerating(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-label="Exportar proyecto"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-neutral-900">Exportar proyecto</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="text-neutral-400 hover:text-neutral-700 transition"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {loading ? (
          <div className="py-10 text-center text-sm text-neutral-500">Cargando opciones…</div>
        ) : (
          <>
            <p className="text-sm text-neutral-500 mb-4">
              {projectTitle} — la información proviene de tu proyecto, no del historial de chat.
            </p>

            <fieldset className="space-y-2 mb-4">
              <legend className="text-sm font-medium text-neutral-700 mb-2">
                ¿Qué quieres descargar?
              </legend>
              <label className="flex items-start gap-2 text-sm text-neutral-700 cursor-pointer">
                <input
                  type="radio"
                  name="option"
                  value="complete"
                  checked={option === "complete"}
                  onChange={() => setOption("complete")}
                  className="mt-0.5"
                />
                <span>
                  <span className="font-medium">Proyecto completo</span>
                  <span className="block text-xs text-neutral-500">
                    Exporta toda la información estructurada disponible del proyecto.
                  </span>
                </span>
              </label>
              <label className="flex items-start gap-2 text-sm text-neutral-700 cursor-pointer">
                <input
                  type="radio"
                  name="option"
                  value="template"
                  checked={option === "template"}
                  onChange={() => setOption("template")}
                  className="mt-0.5"
                />
                <span>
                  <span className="font-medium">Proyecto usando plantilla</span>
                  <span className="block text-xs text-neutral-500">
                    Utiliza un documento cargado como referencia para organizar la estructura.
                  </span>
                </span>
              </label>
              <label className="flex items-start gap-2 text-sm text-neutral-700 cursor-pointer">
                <input
                  type="radio"
                  name="option"
                  value="summary"
                  checked={option === "summary"}
                  onChange={() => setOption("summary")}
                  className="mt-0.5"
                />
                <span>
                  <span className="font-medium">Solo resumen</span>
                  <span className="block text-xs text-neutral-500">
                    Resumen ejecutivo y métricas del proyecto.
                  </span>
                </span>
              </label>
            </fieldset>

            {option === "template" && (
              <div className="mb-4">
                <label htmlFor="template" className="text-sm font-medium text-neutral-700 block mb-1">
                  Documento de referencia
                </label>
                {documents.length === 0 ? (
                  <p className="text-xs text-neutral-500">
                    No hay documentos procesados disponibles. Sube uno desde “Información del proyecto”.
                  </p>
                ) : (
                  <select
                    id="template"
                    value={templateId}
                    onChange={(e) => setTemplateId(e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  >
                    <option value="">Seleccionar documento…</option>
                    {documents.map((doc) => (
                      <option key={doc.id} value={doc.id}>
                        📄 {doc.filename}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            )}

            <fieldset className="space-y-2 mb-5">
              <legend className="text-sm font-medium text-neutral-700 mb-2">Formato</legend>
              {(["DOCX", "PDF", "MARKDOWN"] as ExportFormat[]).map((f) => (
                <label key={f} className="flex items-center gap-2 text-sm text-neutral-700 cursor-pointer">
                  <input
                    type="radio"
                    name="format"
                    value={f}
                    checked={format === f}
                    onChange={() => setFormat(f)}
                  />
                  {FORMAT_LABELS[f]}
                </label>
              ))}
            </fieldset>

            {status && (
              <p
                className={`text-sm mb-4 ${status.type === "error" ? "text-red-600" : "text-emerald-700"}`}
                role={status.type === "error" ? "alert" : "status"}
              >
                {status.text}
              </p>
            )}

            {option === "template" && templateApplied && (
              <p className="text-sm mb-4 text-emerald-700" role="status">
                Estructura aplicada: ✓ Sí
              </p>
            )}

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                disabled={generating}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleDownload}
                disabled={!canDownload}
                className="rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {generating ? "Generando…" : "Descargar"}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
