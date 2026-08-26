"use client";

import { useEffect, useState } from "react";
import AlertList from "@/components/physician/AlertList";
import FeedbackButton from "@/components/health/FeedbackButton";
import PatientDetailView from "@/components/physician/PatientDetailView";
import PatientList from "@/components/physician/PatientList";
import { physicianService } from "@/services/physician";
import type { ClinicalAlert, PhysicianPatientSummary } from "@/services/physician";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "@/services/triage";

export default function PhysicianDashboard() {
  const [page, setPage] = useState<PageResponse<PhysicianPatientSummary> | null>(null);
  const [alerts, setAlerts] = useState<ClinicalAlert[]>([]);
  const [selectedSummary, setSelectedSummary] = useState<PhysicianPatientSummary | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<TriageHistoryEntry[]>([]);
  const [error, setError] = useState("");

  const load = (pageNumber: number) =>
    Promise.all([
      physicianService.patients(pageNumber, 10),
      physicianService.alerts(),
    ]);

  useEffect(() => {
    let cancelled = false;
    load(0)
      .then(([patients, alertsData]) => {
        if (cancelled) return;
        setPage(patients);
        setAlerts(alertsData);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handlePageChange = async (pageNumber: number) => {
    if (pageNumber < 0) return;
    try {
      const [patients] = await load(pageNumber);
      setPage(patients);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleSelect = async (patientId: string) => {
    try {
      const [summary, history] = await Promise.all([
        physicianService.patientSummary(patientId),
        physicianService.patientHistory(patientId),
      ]);
      setSelectedSummary(summary);
      setSelectedHistory(history);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const handleAcknowledge = async (alertId: string) => {
    try {
      await physicianService.acknowledgeAlert(alertId);
      setAlerts((prev) => prev.filter((a) => a.id !== alertId));
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Portal Médico</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Pacientes asignados, resúmenes clínicos y alertas de alta urgencia.
            </p>
          </div>
          <FeedbackButton label="Dar feedback" />
        </div>

        <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          ⚠️ Información clínica de apoyo. La decisión médica final es siempre
          del profesional responsable del paciente.
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
        )}

        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Alertas de alta urgencia</h2>
          <AlertList alerts={alerts} onAcknowledge={handleAcknowledge} />
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Mis pacientes</h2>
          {page && (
            <PatientList page={page} onPageChange={handlePageChange} onSelect={handleSelect} />
          )}
        </section>
      </div>

      {selectedSummary && (
        <PatientDetailView
          summary={selectedSummary}
          history={selectedHistory}
          onClose={() => setSelectedSummary(null)}
        />
      )}
    </main>
  );
}
