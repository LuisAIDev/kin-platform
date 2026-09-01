"use client";

import { useEffect, useState } from "react";
import { documentsService, type ClinicalDocument } from "@/services/documents";
import { physicianService, type PhysicianPatientSummary } from "@/services/physician";

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", { day: "numeric", month: "short", year: "numeric" });
}

export default function PhysicianDocumentsPage() {
  const [patients, setPatients] = useState<PhysicianPatientSummary[]>([]);
  const [selectedPatient, setSelectedPatient] = useState("");
  const [documents, setDocuments] = useState<ClinicalDocument[]>([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Subida
  const [file, setFile] = useState<File | null>(null);
  const [description, setDescription] = useState("");
  const [uploading, setUploading] = useState(false);

  const loadDocuments = async (patientId: string) => {
    if (!patientId) return;
    try {
      const data = await documentsService.documentsForPatient(patientId);
      setDocuments(data);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    physicianService
      .patients(0, 100, "ACTIVE")
      .then((data) => {
        if (cancelled) return;
        setPatients(data.content);
        if (data.content.length > 0) setSelectedPatient(data.content[0].patientId);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedPatient) return;
    let cancelled = false;
    documentsService
      .documentsForPatient(selectedPatient)
      .then((data) => {
        if (!cancelled) setDocuments(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedPatient]);

  const handleUpload = async () => {
    if (!file || !selectedPatient) return;
    setUploading(true);
    setError("");
    setMessage("");
    try {
      await documentsService.upload(file, selectedPatient, description.trim() || undefined);
      setMessage("Documento subido correctamente.");
      setFile(null);
      setDescription("");
      await loadDocuments(selectedPatient);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (documentId: string) => {
    setError("");
    setMessage("");
    try {
      await documentsService.delete(documentId);
      setMessage("Documento eliminado.");
      await loadDocuments(selectedPatient);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Documentos clínicos</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Sube, consulta y gestiona documentos de tus pacientes con relación activa.
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}
        {message && <p className="text-sm text-emerald-700 bg-emerald-50 px-4 py-2.5 rounded-lg">{message}</p>}

        <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
          <h2 className="text-base font-semibold">Seleccionar paciente</h2>
          {patients.length === 0 ? (
            <p className="text-sm text-neutral-500">
              No tienes pacientes con relación activa.
            </p>
          ) : (
            <select
              value={selectedPatient}
              onChange={(e) => setSelectedPatient(e.target.value)}
              aria-label="Paciente"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {patients.map((p) => (
                <option key={p.patientId} value={p.patientId}>
                  {p.patientName}
                </option>
              ))}
            </select>
          )}
        </section>

        <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
          <h2 className="text-base font-semibold">Subir documento</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <input
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              aria-label="Archivo"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
            />
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Descripción (opcional)"
              aria-label="Descripción"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <button
            type="button"
            onClick={handleUpload}
            disabled={!file || !selectedPatient || uploading}
            className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
          >
            {uploading ? "Subiendo..." : "Subir documento"}
          </button>
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-base font-semibold">Documentos del paciente</h2>
          {documents.length === 0 ? (
            <p className="text-sm text-neutral-500">Sin documentos para este paciente.</p>
          ) : (
            <div className="flex flex-col gap-2">
              {documents.map((d) => (
                <div
                  key={d.id}
                  className="rounded-xl border border-neutral-200 bg-white p-4 flex flex-wrap items-center gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-neutral-800">{d.fileName}</p>
                    <p className="text-xs text-neutral-500">
                      {formatSize(d.fileSize)} · {formatDate(d.uploadedAt)}
                      {d.description ? ` · ${d.description}` : ""}
                    </p>
                  </div>
                  <a
                    href={documentsService.downloadUrl(d.id)}
                    className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 transition"
                  >
                    Descargar
                  </a>
                  <button
                    type="button"
                    onClick={() => handleDelete(d.id)}
                    className="rounded-lg border border-red-200 text-red-700 px-3 py-1.5 text-xs font-medium hover:bg-red-50 transition"
                  >
                    Eliminar
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
