"use client";

import { useEffect, useState } from "react";
import { documentsService, type ClinicalDocument } from "@/services/documents";

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", { day: "numeric", month: "short", year: "numeric" });
}

export default function PatientDocumentsPage() {
  const [documents, setDocuments] = useState<ClinicalDocument[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    documentsService
      .myDocuments()
      .then((data) => {
        if (!cancelled) setDocuments(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Mis documentos</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Documentos clínicos compartidos por tu médico.
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

        <section className="flex flex-col gap-3">
          {documents.length === 0 ? (
            <p className="text-sm text-neutral-500">Aún no tienes documentos.</p>
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
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
