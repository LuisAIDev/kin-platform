"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import DocumentChatPanel from "@/components/health/documents/DocumentChatPanel";
import { documentsService, type ClinicalDocument } from "@/services/documents";
import { Camera, Trash2 } from "lucide-react";

const ALLOWED_EXTENSIONS = ["pdf", "docx", "xlsx", "txt", "csv", "jpg", "jpeg", "png", "heic", "webp"];
const MAX_FILE_SIZE = 10 * 1024 * 1024;

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", { day: "numeric", month: "short", year: "numeric" });
}

function validateFile(file: File): string {
  const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return "Formato no permitido. Sube PDF, DOCX, XLSX, TXT, CSV, JPG, PNG, HEIC o WebP.";
  }
  if (file.size > MAX_FILE_SIZE) {
    return "El archivo supera el tamaño máximo de 10 MB.";
  }
  return "";
}

export default function PatientDocumentsPage() {
  const [documents, setDocuments] = useState<ClinicalDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [selectedId, setSelectedId] = useState<string>("");

  const [showUpload, setShowUpload] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [description, setDescription] = useState("");
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const loadDocuments = useCallback(async (preferredId?: string) => {
    try {
      const data = await documentsService.myDocuments();
      setDocuments(data);
      setSelectedId(preferredId ?? (data[0]?.id ?? ""));
    } catch (err) {
      setError((err as Error).message);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    documentsService
      .myDocuments()
      .then((data) => {
        if (cancelled) return;
        setDocuments(data);
        setSelectedId(data[0]?.id ?? "");
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleUpload = async () => {
    if (!file) return;
    const validation = validateFile(file);
    if (validation) {
      setUploadError(validation);
      return;
    }
    setUploading(true);
    setUploadError("");
    setMessage("");
    try {
      const uploaded = await documentsService.myUpload(file, description.trim() || undefined);
      setFile(null);
      setDescription("");
      setShowUpload(false);
      setMessage("Documento subido. Ya puedes analizarlo con IA en el panel de la derecha.");
      await loadDocuments(uploaded.id);
    } catch (err) {
      setUploadError((err as Error).message);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (documentId: string) => {
    const confirmed = confirm(
      "¿Eliminar este documento de tu Centro de Documentos?\n\n" +
        "• Se ocultará de TU vista.\n" +
        "• Seguirá disponible para TU MÉDICO y auditoría médica.\n" +
        "• El archivo se conserva cifrado.\n\n" +
        "¿Continuar?"
    );

    if (!confirmed) return;

    setError("");
    setMessage("");
    try {
      await documentsService.delete(documentId);
      setMessage("Documento eliminado.");
      await loadDocuments();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const selected = documents.find((d) => d.id === selectedId) ?? null;

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-8 pb-12">
      <div className="w-full max-w-6xl flex flex-col gap-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Centro de Documentos Clínicos</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Guarda tus exámenes, importa sus datos relevantes y analízalos con IA en lenguaje
              sencillo.
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              setShowUpload((v) => !v);
              setUploadError("");
            }}
            className="rounded-xl bg-primary-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-primary-700 transition"
          >
            {showUpload ? "Cancelar" : "+ Agregar documento"}
          </button>
        </div>

        <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          ⚠️ Tus documentos y su análisis son privados y solo visibles para ti. Esta información es de
          apoyo informativo y no sustituye la evaluación de un profesional de la salud.
        </div>

        {showUpload && (
          <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
            <h2 className="text-base font-semibold">Agregar documento</h2>
            <p className="text-xs text-neutral-500">
              Formatos admitidos: PDF, DOCX, XLSX, TXT, CSV, JPG, PNG, HEIC, WebP · máximo 10 MB.
              Las fotos se procesan con OCR para extraer el texto automáticamente.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input
                type="file"
                accept=".pdf,.docx,.xlsx,.txt,.csv,image/jpeg,image/png,image/heic,image/webp"
                onChange={(e) => {
                  setFile(e.target.files?.[0] ?? null);
                  setUploadError("");
                }}
                aria-label="Archivo a subir"
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
              />
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Descripción (opcional)"
                aria-label="Descripción del documento"
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <div className="flex gap-3">
              <input
                ref={cameraInputRef}
                type="file"
                accept="image/*"
                capture="environment"
                onChange={(e) => {
                  setFile(e.target.files?.[0] ?? null);
                  setUploadError("");
                }}
                aria-label="Tomar foto con la cámara"
                className="hidden"
              />
              <button
                type="button"
                onClick={() => cameraInputRef.current?.click()}
                className="inline-flex items-center gap-2 rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition"
              >
                <Camera className="w-4 h-4" />
                Tomar foto
              </button>
            </div>
            {uploadError && <p className="text-sm text-red-600">{uploadError}</p>}
            <div>
              <button
                type="button"
                onClick={handleUpload}
                disabled={!file || uploading}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
              >
                {uploading ? "Subiendo…" : "Guardar documento"}
              </button>
            </div>
          </section>
        )}

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}
        {message && (
          <p className="text-sm text-emerald-700 bg-emerald-50 px-4 py-2.5 rounded-lg">{message}</p>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 items-start">
          <section className="lg:col-span-2 flex flex-col gap-3">
            <h2 className="text-base font-semibold">Mis documentos</h2>
            {loading ? (
              <p className="text-sm text-neutral-500">Cargando documentos...</p>
            ) : documents.length === 0 ? (
              <div className="rounded-xl border border-dashed border-neutral-300 p-6 text-center">
                <p className="text-sm text-neutral-500 mb-2">Aún no tienes documentos.</p>
                <p className="text-xs text-neutral-400">
                  Pulsa “Agregar documento” para subir tu primer examen.
                </p>
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                {documents.map((d) => (
                  <button
                    key={d.id}
                    type="button"
                    onClick={() => setSelectedId(d.id)}
                    className={`text-left rounded-xl border p-4 flex flex-col gap-1.5 transition ${
                      d.id === selectedId
                        ? "border-primary-500 bg-primary-50"
                        : "border-neutral-200 bg-white hover:border-primary-300"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-semibold text-neutral-800 truncate">{d.fileName}</p>
                      {d.analyzable ? (
                        <span className="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-medium text-emerald-700">
                          Analizable
                        </span>
                      ) : (
                        <span className="shrink-0 rounded-full bg-neutral-100 px-2 py-0.5 text-[10px] font-medium text-neutral-500">
                          Sin texto
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-neutral-500">
                      {formatSize(d.fileSize)} · {formatDate(d.uploadedAt)}
                      {d.description ? ` · ${d.description}` : ""}
                    </p>
                    <div className="flex items-center gap-2 mt-1" onClick={(e) => e.stopPropagation()}>
                      <a
                        href={documentsService.downloadUrl(d.id)}
                        onClick={(e) => e.stopPropagation()}
                        className="rounded-lg bg-neutral-100 px-2.5 py-1 text-xs font-medium text-neutral-700 hover:bg-neutral-200 transition"
                      >
                        Descargar
                      </a>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(d.id);
                        }}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition"
                        aria-label="Eliminar este documento de mi centro"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Eliminar
                      </button>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </section>

          <section className="lg:col-span-3">
            {selected ? (
              <DocumentChatPanel key={selected.id} document={selected} />
            ) : (
              <div className="rounded-xl border border-dashed border-neutral-300 p-10 text-center text-sm text-neutral-500">
                Selecciona un documento para analizarlo con IA, importar sus datos o descargar el
                análisis.
              </div>
            )}
          </section>
        </div>
      </div>
    </main>
  );
}
