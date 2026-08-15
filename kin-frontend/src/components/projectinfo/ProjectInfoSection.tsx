"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  projectInfoService,
  type InfoEntryInput,
  type ProjectDocumentItem,
  type StructuredInfoEntry,
} from "@/services/projectInfo";

const ALLOWED_EXTENSIONS = ["pdf", "docx", "xlsx", "txt", "csv"];
const MAX_FILE_BYTES = 10 * 1024 * 1024;

const INFO_SECTIONS: { section: string; fields: { key: string; label: string }[] }[] = [
  {
    section: "DATOS_GENERALES",
    fields: [
      { key: "nombre", label: "Nombre" },
      { key: "descripcion", label: "Descripción" },
      { key: "sector", label: "Sector" },
      { key: "ubicacion", label: "Ubicación" },
      { key: "problema", label: "Problema que resuelve" },
      { key: "publico_objetivo", label: "Público objetivo" },
    ],
  },
  {
    section: "MODELO_DE_NEGOCIO",
    fields: [
      { key: "producto_servicio", label: "Producto/servicio" },
      { key: "propuesta_de_valor", label: "Propuesta de valor" },
      { key: "clientes", label: "Clientes" },
      { key: "canales", label: "Canales" },
      { key: "fuentes_de_ingresos", label: "Fuentes de ingresos" },
      { key: "recursos_clave", label: "Recursos clave" },
      { key: "actividades_clave", label: "Actividades clave" },
      { key: "socios_clave", label: "Socios clave" },
    ],
  },
  {
    section: "FINANZAS",
    fields: [
      { key: "inversion_inicial", label: "Inversión inicial" },
      { key: "costos_fijos", label: "Costos fijos" },
      { key: "costos_variables", label: "Costos variables" },
      { key: "precio", label: "Precio" },
      { key: "ventas_estimadas", label: "Ventas estimadas" },
      { key: "proyeccion_ingresos", label: "Proyección de ingresos" },
    ],
  },
  {
    section: "OPERACION",
    fields: [
      { key: "empleados", label: "Número de empleados" },
      { key: "capacidad_produccion", label: "Capacidad de producción" },
      { key: "proveedores", label: "Proveedores" },
      { key: "equipos", label: "Equipos" },
      { key: "infraestructura", label: "Infraestructura" },
    ],
  },
  {
    section: "MERCADO",
    fields: [
      { key: "mercado_objetivo", label: "Mercado objetivo" },
      { key: "competidores", label: "Competidores conocidos" },
      { key: "diferenciadores", label: "Diferenciadores" },
    ],
  },
  {
    section: "IMPACTO",
    fields: [
      { key: "impacto_social", label: "Impacto social" },
      { key: "impacto_ambiental", label: "Impacto ambiental" },
      { key: "generacion_empleo", label: "Generación de empleo" },
      { key: "beneficiarios", label: "Beneficiarios" },
    ],
  },
  {
    section: "RIESGOS",
    fields: [
      { key: "riesgos_identificados", label: "Riesgos identificados" },
      { key: "probabilidad", label: "Probabilidad" },
      { key: "impacto", label: "Impacto" },
      { key: "mitigacion", label: "Estrategia de mitigación" },
    ],
  },
];

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString("es-ES", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  } catch {
    return iso;
  }
}

const STATUS_STYLE: Record<string, string> = {
  PROCESADO: "bg-emerald-100 text-emerald-700",
  PROCESANDO: "bg-amber-100 text-amber-700",
  PENDIENTE: "bg-neutral-100 text-neutral-600",
  ERROR: "bg-red-100 text-red-700",
};

const STATUS_LABEL: Record<string, string> = {
  PROCESADO: "Procesado correctamente",
  PROCESANDO: "Procesando...",
  PENDIENTE: "Pendiente",
  ERROR: "Error al procesar",
};

interface ProjectInfoSectionProps {
  projectId: string;
}

/** Sección "INFORMACIÓN DEL PROYECTO" (M3I): importar información estructurada y agregar documentos. */
export function ProjectInfoSection({ projectId }: ProjectInfoSectionProps) {
  const [importOpen, setImportOpen] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [entries, setEntries] = useState<Record<string, string>>({});
  const [info, setInfo] = useState<StructuredInfoEntry[]>([]);
  const [documents, setDocuments] = useState<ProjectDocumentItem[]>([]);
  const [saving, setSaving] = useState(false);
  const [infoMessage, setInfoMessage] = useState<string | null>(null);
  const [infoError, setInfoError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState<string | null>(null);

  const refresh = useCallback(() => {
    projectInfoService
      .listInfo(projectId)
      .then((data) => {
        setInfo(data);
        const prefilled: Record<string, string> = {};
        for (const entry of data) {
          const idx = INFO_SECTIONS.findIndex((s) => s.section === entry.section);
          if (idx >= 0) prefilled[`${entry.section}.${entry.key}`] = entry.value;
        }
        setEntries((prev) => ({ ...prefilled, ...prev }));
      })
      .catch(() => setInfo([]));
    projectInfoService
      .listDocuments(projectId)
      .then(setDocuments)
      .catch(() => setDocuments([]));
  }, [projectId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const allFields = useMemo(
    () =>
      INFO_SECTIONS.flatMap((s) => s.fields.map((f) => ({ section: s.section, ...f }))),
    [],
  );

  const handleSaveInfo = async () => {
    setSaving(true);
    setInfoError(null);
    setInfoMessage(null);
    const payload: InfoEntryInput[] = allFields
      .filter((f) => (entries[`${f.section}.${f.key}`] ?? "").trim() !== "")
      .map((f) => ({
        section: f.section,
        key: f.key,
        value: (entries[`${f.section}.${f.key}`] ?? "").trim(),
        sourceType: "USER_INPUT" as const,
      }));
    try {
      await projectInfoService.saveInfo(projectId, payload);
      setInfoMessage(`Se guardaron ${payload.length} campos como información del proyecto.`);
      setImportOpen(false);
      refresh();
    } catch (err) {
      setInfoError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  };

  const handleSelectFile = (file: File | null) => {
    setFileError(null);
    setUploadResult(null);
    setSelectedFile(file);
    if (!file) return;
    const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      setFileError("Formato no permitido. Usa PDF, DOCX, XLSX, TXT o CSV.");
      setSelectedFile(null);
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      setFileError("El archivo supera el tamaño máximo de 10 MB.");
      setSelectedFile(null);
      return;
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    setUploading(true);
    setFileError(null);
    setUploadResult(null);
    try {
      const doc = await projectInfoService.uploadDocument(projectId, selectedFile);
      setUploadResult(
        doc.status === "PROCESADO"
          ? "Documento procesado correctamente."
          : doc.status === "ERROR"
            ? "Error al procesar el documento."
            : "Documento subido y en procesamiento.",
      );
      setUploadOpen(false);
      refresh();
    } catch (err) {
      setFileError(err instanceof Error ? err.message : String(err));
    } finally {
      setUploading(false);
    }
  };

  const sectionTitleStyle = "text-xs font-semibold uppercase tracking-wider text-neutral-400 mb-2";

  return (
    <div className="mt-6 border-t border-neutral-200 pt-4">
      <h3 className={sectionTitleStyle}>Información del proyecto</h3>
      <div className="space-y-2">
        <button
          type="button"
          onClick={() => setImportOpen(true)}
          className="block w-full rounded-xl bg-white border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-800 text-center hover:bg-neutral-50 transition"
        >
          📋 Importar información
        </button>
        <button
          type="button"
          onClick={() => setUploadOpen(true)}
          className="block w-full rounded-xl bg-white border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-800 text-center hover:bg-neutral-50 transition"
        >
          📎 Agregar documento
        </button>
      </div>

      {documents.length > 0 && (
        <ul className="mt-4 space-y-2" data-testid="document-list">
          {documents.map((doc) => (
            <li key={doc.id} className="text-xs text-neutral-600 border border-neutral-200 rounded-lg p-2">
              <span className="font-medium text-neutral-800 break-all">📄 {doc.filename}</span>
              <div className="flex items-center justify-between mt-1">
                <span className={`text-[10px] font-medium px-1.5 py-0.5 rounded-full ${STATUS_STYLE[doc.status] ?? STATUS_STYLE.PENDIENTE}`}>
                  {STATUS_LABEL[doc.status] ?? doc.status}
                </span>
                <span className="text-neutral-400">
                  {formatBytes(doc.size)} · {formatDate(doc.createdAt)}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}

      {info.length > 0 && (
        <div className="mt-4" data-testid="info-list">
          <p className="text-[10px] font-semibold text-neutral-400 uppercase tracking-wide mb-1">
            {info.length} dato{info.length === 1 ? "" : "s"} registrado{info.length === 1 ? "" : "s"}
          </p>
          <p className="text-xs text-neutral-500">Los datos importados quedan disponibles como contexto del proyecto.</p>
        </div>
      )}

      {importOpen && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
          <div className="bg-white rounded-2xl w-full max-w-2xl max-h-[85vh] overflow-y-auto p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold">📋 Importar información del proyecto</h3>
              <button type="button" onClick={() => setImportOpen(false)} className="text-neutral-400 hover:text-neutral-700" aria-label="Cerrar">
                ✕
              </button>
            </div>
            <p className="text-xs text-neutral-500 mb-4">
              Registra datos estructurados sin escribirlos en el chat. Los campos vacíos no se guardan.
            </p>

            {infoError && <p className="text-xs text-red-600 mb-3">{infoError}</p>}

            {INFO_SECTIONS.map((section) => (
              <fieldset key={section.section} className="mb-4">
                <legend className="text-xs font-semibold uppercase tracking-wider text-primary-600 mb-2">
                  {section.section.replace(/_/g, " ")}
                </legend>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {section.fields.map((field) => (
                    <label key={field.key} className="flex flex-col gap-1 text-xs text-neutral-600">
                      {field.label}
                      <input
                        type="text"
                        value={entries[`${section.section}.${field.key}`] ?? ""}
                        onChange={(e) =>
                          setEntries((prev) => ({
                            ...prev,
                            [`${section.section}.${field.key}`]: e.target.value,
                          }))
                        }
                        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                      />
                    </label>
                  ))}
                </div>
              </fieldset>
            ))}

            <div className="flex justify-end gap-2 mt-4">
              <button
                type="button"
                onClick={() => setImportOpen(false)}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => void handleSaveInfo()}
                disabled={saving}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-50"
              >
                {saving ? "Guardando..." : "Guardar información"}
              </button>
            </div>
            {infoMessage && <p className="text-xs text-emerald-600 mt-3">{infoMessage}</p>}
          </div>
        </div>
      )}

      {uploadOpen && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
          <div className="bg-white rounded-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold">📎 Agregar documento</h3>
              <button type="button" onClick={() => setUploadOpen(false)} className="text-neutral-400 hover:text-neutral-700" aria-label="Cerrar">
                ✕
              </button>
            </div>
            <p className="text-xs text-neutral-500 mb-4">
              El documento se procesa como información del proyecto, no se envía como mensaje de chat.
            </p>

            <input
              type="file"
              accept=".pdf,.docx,.xlsx,.txt,.csv"
              data-testid="document-file-input"
              onChange={(e) => handleSelectFile(e.target.files?.[0] ?? null)}
              className="text-sm text-neutral-600"
            />

            {selectedFile && (
              <div className="mt-3 text-xs text-neutral-700 border border-neutral-200 rounded-lg p-2">
                📄 {selectedFile.name}
                <span className="text-neutral-400"> · {formatBytes(selectedFile.size)}</span>
              </div>
            )}
            {fileError && <p className="text-xs text-red-600 mt-2">{fileError}</p>}
            {uploadResult && <p className="text-xs text-emerald-600 mt-2">{uploadResult}</p>}

            <div className="flex justify-end gap-2 mt-5">
              <button
                type="button"
                onClick={() => setUploadOpen(false)}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => void handleUpload()}
                disabled={!selectedFile || uploading}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-50"
              >
                {uploading ? "Subiendo..." : "Subir y procesar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
