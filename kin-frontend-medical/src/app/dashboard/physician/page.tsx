"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import AlertList from "@/components/physician/AlertList";
import AIProgressIndicator from "@/components/AIProgressIndicator";
import AIResultModal from "@/components/AIResultModal";
import FeedbackButton from "@/components/health/FeedbackButton";
import InvitePatientModal from "@/components/physician/InvitePatientModal";
import PatientDetailView from "@/components/physician/PatientDetailView";
import PatientList from "@/components/physician/PatientList";
import PhysicianApplicationWidget from "@/components/physician/PhysicianApplicationWidget";
import { aiassistService } from "@/services/aiassist";
import { physicianService } from "@/services/physician";
import type { ClinicalAlert, PhysicianPatientSummary } from "@/services/physician";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "@/services/triage";

type PatientFilter = "ACTIVE" | "PENDING" | "ALL";

const FILTERS: { value: PatientFilter; label: string }[] = [
  { value: "ACTIVE", label: "Activos" },
  { value: "PENDING", label: "Pendientes" },
  { value: "ALL", label: "Todos" },
];

export default function PhysicianDashboard() {
  const [filter, setFilter] = useState<PatientFilter>("ACTIVE");
  const [page, setPage] = useState<PageResponse<PhysicianPatientSummary> | null>(null);
  const [alerts, setAlerts] = useState<ClinicalAlert[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(null);
  const [selectedSummary, setSelectedSummary] = useState<PhysicianPatientSummary | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<TriageHistoryEntry[]>([]);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [error, setError] = useState("");

  const load = (pageNumber: number, status: PatientFilter) =>
    Promise.all([
      physicianService.patients(pageNumber, 10, status),
      physicianService.alerts(),
    ]);

  useEffect(() => {
    let cancelled = false;
    load(0, filter)
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
  }, [filter]);

  const handlePageChange = async (pageNumber: number) => {
    if (pageNumber < 0) return;
    try {
      const [patients] = await load(pageNumber, filter);
      setPage(patients);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const [aiResult, setAiResult] = useState<{ type: string; response: string } | null>(null);
  const [aiLoading, setAiLoading] = useState(false);

  const handleSelect = (patientId: string) => {
    setSelectedPatientId(patientId);
    setError("");
    physicianService.patientSummary(patientId).then((summary) => {
      setSelectedSummary(summary);
    });
    physicianService.patientHistory(patientId).then((history) => {
      setSelectedHistory(history);
    }).catch((err) => {
      setError((err as Error).message);
    });
  };

  const clearSelection = () => {
    setSelectedPatientId(null);
    setSelectedSummary(null);
    setSelectedHistory([]);
  };

  const handleAiAssist = async (type: string, patientId: string) => {
    setAiResult(null);
    setAiLoading(true);
    try {
      let response: any;
      switch (type) {
        case "summary":
          response = await aiassistService.generateSummary(patientId);
          break;
        case "prepare":
          response = await aiassistService.prepareConsultation(patientId);
          break;
        case "draft":
          response = await aiassistService.draftMessage(
            patientId,
            ""
          );
          break;
      }
      setAiResult({ type, response: response.response });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setAiLoading(false);
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

  const handleInvited = async () => {
    try {
      const [patients] = await load(0, filter);
      setPage(patients);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        {/* Solicitud de capacidad profesional para cuentas existentes (solo en portal médico). */}
        <PhysicianApplicationWidget />
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Portal Médico</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Pacientes asignados, resúmenes clínicos y alertas de alta urgencia.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/dashboard/physician/encounters/new"
              className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 transition"
            >
              Nueva consulta
            </Link>
            {selectedPatientId && (
              <Link
                href={`/dashboard/physician/patients/${selectedPatientId}/encounters`}
                className="rounded-lg border border-primary-200 px-3 py-1.5 text-xs font-medium text-primary-700 hover:bg-primary-50 transition"
              >
                Historial de consultas
              </Link>
            )}
            <button
              type="button"
              disabled={!selectedPatientId}
              onClick={() => {
                if (!selectedPatientId) return;
                handleAiAssist("summary", selectedPatientId);
              }}
              className="rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Generar resumen
            </button>
            <button
              type="button"
              disabled={!selectedPatientId}
              onClick={() => {
                if (!selectedPatientId) return;
                handleAiAssist("prepare", selectedPatientId);
              }}
              className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Preparar consulta
            </button>
            <button
              type="button"
              disabled={!selectedPatientId}
              onClick={() => {
                if (!selectedPatientId) return;
                handleAiAssist("draft", selectedPatientId);
              }}
              className="rounded-lg bg-purple-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-purple-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Redactar mensaje
            </button>
            <button
              type="button"
              onClick={() => setShowInviteModal(true)}
              className="rounded-lg bg-neutral-800 px-3 py-1.5 text-xs font-medium text-white hover:bg-neutral-900 transition"
            >
              Invitar paciente
            </button>
            <FeedbackButton label="Dar feedback" />
          </div>
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
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Mis pacientes</h2>
            <div className="flex rounded-lg border border-neutral-200 bg-neutral-50 p-0.5 gap-0.5">
              {FILTERS.map((f) => (
                <button
                  key={f.value}
                  type="button"
                  onClick={() => setFilter(f.value)}
                  aria-pressed={filter === f.value}
                  className={`rounded-md px-3 py-1.5 text-xs font-semibold transition ${
                    filter === f.value
                      ? "bg-primary-600 text-white shadow-sm"
                      : "text-neutral-600 hover:bg-white"
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>
          </div>
          {page && (
            <PatientList
              page={page}
              onPageChange={handlePageChange}
              onSelect={handleSelect}
              selectedPatientId={selectedPatientId}
            />
          )}
        </section>
      </div>

      {selectedSummary && (
        <PatientDetailView
          summary={selectedSummary}
          history={selectedHistory}
          onClose={clearSelection}
        />
      )}

      {showInviteModal && (
        <InvitePatientModal onClose={() => setShowInviteModal(false)} onInvited={handleInvited} />
      )}

      {aiResult && (
        <AIResultModal
          open={true}
          onClose={() => setAiResult(null)}
          type={aiResult.type as any}
          title="Resultado de IA"
          response={aiResult.response}
          onCopy={() => {/* copiar al portapapeles */}}
        />
      )}

      <AIProgressIndicator visible={aiLoading} onComplete={() => setAiLoading(false)} />
    </main>
  );
}
