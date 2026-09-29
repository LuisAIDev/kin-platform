"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import EncounterCreateForm from "@/components/hce/EncounterCreateForm";
import { physicianService } from "@/services/physician";
import type { PhysicianPatientSummary } from "@/services/physician";
import type { EncounterResponse } from "@/lib/hce/api/hce.api";

function NewEncounterContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const defaultPatientId = searchParams.get("patientId") ?? undefined;

  const [patients, setPatients] = useState<PhysicianPatientSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    physicianService
      .patients(0, 100, "ALL")
      .then((page) => {
        if (!cancelled) setPatients(page.content);
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

  const handleCreated = (encounter: EncounterResponse) => {
    router.push(`/dashboard/physician/hce/${encounter.id}/edit`);
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-2xl flex flex-col gap-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Nueva consulta</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Crea un encuentro clínico para un paciente de tu cartera.
            </p>
          </div>
          <Link
            href="/dashboard/physician"
            className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100 transition"
          >
            Volver
          </Link>
        </div>

        {error && (
          <p role="alert" className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">
            {error}
          </p>
        )}

        {loading ? (
          <p className="text-sm text-neutral-500">Cargando pacientes...</p>
        ) : (
          <EncounterCreateForm
            patients={patients}
            defaultPatientId={defaultPatientId}
            onCreated={handleCreated}
          />
        )}
      </div>
    </main>
  );
}

export default function NewEncounterPage() {
  return (
    <Suspense
      fallback={<div className="p-8 text-center text-sm text-neutral-500">Cargando...</div>}
    >
      <NewEncounterContent />
    </Suspense>
  );
}
