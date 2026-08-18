"use client";

import { useRef, useState } from "react";
import { downloadBlob } from "@/services/enterpriseApi";
import {
  exportProjectService,
  type ExportAction,
  type ExportFormat,
} from "@/services/exportProject";

interface Props {
  action: ExportAction;
  projectId: string;
  projectTitle: string;
}

const FORMAT_LABEL: Record<ExportFormat, string> = {
  DOCX: "Word",
  PDF: "PDF",
  MARKDOWN: "Markdown",
};

const FORMAT_EXT: Record<ExportFormat, string> = {
  DOCX: "docx",
  PDF: "pdf",
  MARKDOWN: "md",
};

function filenameBase(title: string): string {
  const base = title
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_|_$/g, "");
  return `KIN_${base || "proyecto"}`;
}

/**
 * Tarjeta de descarga del proyecto generada a partir de la acción aditiva
 * {@code ChatResponse.action}. Reutiliza los mismos endpoints de exportación
 * (ownership verificado en el backend); nunca usa el historial como contenido.
 */
export default function ExportChatActionCard({ action, projectId, projectTitle }: Props) {
  const [downloading, setDownloading] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const busy = useRef(false);

  async function handleDownload() {
    if (busy.current) {
      return;
    }
    busy.current = true;
    setDownloading(true);
    setStatus(null);
    const ext = FORMAT_EXT[action.format];
    const filename = `${filenameBase(projectTitle)}.${ext}`;
    try {
      const blob = await exportProjectService.download(
        projectId,
        action.format,
        "COMPLETE",
        action.templateDocumentId ?? undefined,
      );
      downloadBlob(blob, filename);
      setStatus("✓ Proyecto descargado correctamente.");
    } catch {
      setStatus("No se pudo generar el documento. Inténtalo de nuevo.");
    } finally {
      busy.current = false;
      setDownloading(false);
    }
  }

  return (
    <div className="rounded-xl border border-primary-200 bg-primary-50 p-4 my-3">
      <p className="text-sm font-medium text-neutral-900">📄 Proyecto listo para descargar</p>
      {action.templateDocumentId && (
        <p className="text-xs text-neutral-600 mt-1">
          Estructura utilizada: {action.templateDocumentName || "documento de referencia"}
        </p>
      )}
      <div className="flex items-center gap-3 mt-3">
        <button
          type="button"
          onClick={handleDownload}
          disabled={downloading}
          className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {downloading ? "Generando…" : `Descargar ${FORMAT_LABEL[action.format]}`}
        </button>
        {status && <span className="text-xs text-emerald-700">{status}</span>}
      </div>
    </div>
  );
}
