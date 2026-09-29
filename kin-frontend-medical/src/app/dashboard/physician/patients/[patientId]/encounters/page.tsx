"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import EncounterList from "@/components/hce/EncounterList";
import { hceApi, type EncounterResponse } from "@/lib/hce/api/hce.api";
import { physicianService } from "@/services/physician";

export default function PatientEncountersPage() {
  const params = useParams<{ patientId: string }>();
  const patientId = params.patientId;

  const [encounters, setEncounters] = useState<EncounterResponse[]>([]);
  const [patientName, setPatientName] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      hceApi.listEncountersByPatient(patientId),
      // El nombre es opcional para mostrar el encabezado; si falla, se omite.
      physicianService.patientSummary(patientId).catch(() => null),
    ])
      .then(([list, summary]) => {
        if (cancelled) return;
        setEncounters(Array.isArray(list) ? list : []);
        if (summary) setPatientName(summary.patientName);
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
  }, [patientId]);

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-3xl flex flex-col gap-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">
              {patientName ? `Consultas de ${patientName}` : "Historial de consultas"}
            </h1>
            <p className="text-sm text-neutral-500 mt-1">
              Encuentros clínicos previos del paciente.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/dashboard/physician"
              className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100 transition"
            >
              Volver
            </Link>
            <Link
              href={`/dashboard/physician/encounters/new?patientId=${encodeURIComponent(patientId)}`}
              className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 transition"
            >
              Nueva consulta
            </Link>
          </div>
        </div>

        {error && (
          <p role="alert" className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">
            {error}
          </p>
        )}

        {loading ? (
          <p className="text-sm text-neutral-500">Cargando consultas...</p>
        ) : (
          <EncounterList encounters={encounters} />
        )}
      </div>
    </main>
  );
}
