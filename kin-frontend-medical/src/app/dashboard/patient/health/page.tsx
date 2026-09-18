"use client";

import Link from "next/link";
import { useEffect, useState, useCallback } from "react";
import CarePlanView from "@/components/health/CarePlanView";
import FeedbackButton from "@/components/health/FeedbackButton";
import HealthSummaryCards from "@/components/health/HealthSummaryCards";
import HistoryList from "@/components/health/HistoryList";
import ProfileEditor from "@/components/health/ProfileEditor";
import ShareTriageModal from "@/components/health/ShareTriageModal";
import SymptomEvolutionChart from "@/components/health/SymptomEvolutionChart";
import { dashboardService } from "@/services/dashboard";
import { SubscriptionStatusBanner } from "@/components/patient/SubscriptionStatusBanner";
import type { CarePlan, HealthSummary } from "@/services/dashboard";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "@/services/triage";
import { API_URL } from "@/services/api";

export default function PatientHealthPage() {
  const [summary, setSummary] = useState<HealthSummary | null>(null);
  const [carePlan, setCarePlan] = useState<CarePlan | null>(null);
  const [page, setPage] = useState<PageResponse<TriageHistoryEntry> | null>(null);
  const [history, setHistory] = useState<TriageHistoryEntry[]>([]);
  const [selected, setSelected] = useState<TriageHistoryEntry | null>(null);
  const [sharingTriage, setSharingTriage] = useState<TriageHistoryEntry | null>(null);
  const [error, setError] = useState("");

  const loadHistory = useCallback(async (pageNumber = 0, size = 50) => {
    try {
      const historyData = await dashboardService.history(pageNumber, size);
      setPage(historyData);
      setHistory(historyData.content);
    } catch (err) {
      setError((err as Error).message);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      dashboardService.summary(),
      dashboardService.carePlan(),
      dashboardService.history(0, 50),
    ])
      .then(([summaryData, carePlanData, historyData]) => {
        if (cancelled) return;
        setSummary(summaryData);
        setCarePlan(carePlanData);
        setPage(historyData);
        setHistory(historyData.content);
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
    await loadHistory(pageNumber, 10);
  };

  const handleHide = async (entry: TriageHistoryEntry) => {
    const confirmed = confirm(
      "¿Ocultar esta consulta de tu historial?\n\n" +
        "• Se ocultará de TU vista.\n" +
        "• Seguirá disponible para TU MÉDICO.\n" +
        "• No se borra permanentemente.\n\n" +
        "¿Continuar?"
    );

    if (!confirmed) return;

    try {
      const res = await fetch(
        `${API_URL}/medical/triage/consultations/${entry.id}/hide`,
        { method: "DELETE", credentials: "include" }
      );

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "No se pudo ocultar la consulta");
      }

      // Recargar el historial
      await loadHistory(page?.currentPage ?? 0, page?.size ?? 10);
    } catch (err) {
      alert((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Mi Salud</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Resumen, historial y plan de cuidado personalizado.
            </p>
          </div>
          <FeedbackButton label="Dar feedback" />
        </div>

        <SubscriptionStatusBanner />

        <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          ⚠️ Esta información es de apoyo informativo. No sustituye la
          evaluación ni el diagnóstico de un profesional de la salud.
        </div>

        <section className="rounded-xl border border-neutral-200 bg-white p-5">
          <h2 className="text-base font-semibold">Centro de Documentos Clínicos</h2>
          <p className="text-xs text-neutral-500 mt-0.5">
            Sube tus exámenes y analízalos con IA en lenguaje sencillo (apoyo informativo).
          </p>
          <div className="flex flex-wrap gap-3 mt-3">
            <Link
              href="/dashboard/patient/documents"
              className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition"
            >
              + Agregar documento
            </Link>
            <Link
              href="/dashboard/patient/documents"
              className="inline-flex items-center gap-2 rounded-lg border border-emerald-200 text-emerald-700 px-4 py-2 text-sm font-medium hover:bg-emerald-50 transition"
            >
              Analizar con IA / Importar información
            </Link>
            <Link
              href="/dashboard/patient/documents"
              className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition"
            >
              Descargar PDF del análisis
            </Link>
          </div>
        </section>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
        )}

        {summary && <HealthSummaryCards summary={summary} />}

        {history.length > 0 && <SymptomEvolutionChart history={history} />}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ProfileEditor />
          {carePlan && <CarePlanView plan={carePlan} />}
        </div>

        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Historial de consultas</h2>
          {page && (
            <HistoryList
              page={page}
              onPageChange={handlePageChange}
              onSelect={setSelected}
              onShare={setSharingTriage}
              onHide={handleHide}
            />
          )}
        </section>
      </div>

      {selected && (
        <div
          className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
          onClick={() => setSelected(null)}
        >
          <div
            className="bg-white rounded-2xl max-w-lg w-full p-6 flex flex-col gap-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between">
              <h3 className="text-lg font-semibold">Detalle de la consulta</h3>
              <button
                type="button"
                onClick={() => setSelected(null)}
                aria-label="Cerrar"
                className="text-neutral-400 hover:text-neutral-600 text-xl leading-none"
              >
                ×
              </button>
            </div>

            <p className="text-sm text-neutral-500">
              {new Date(selected.createdAt).toLocaleString("es-ES")}
            </p>

            <div className="flex flex-col gap-1.5">
              <span className="text-xs font-semibold uppercase text-neutral-400">Síntomas</span>
              <div className="flex flex-wrap gap-2">
                {selected.symptoms.map((s) => (
                  <span key={s} className="rounded-full bg-neutral-100 px-2.5 py-1 text-xs text-neutral-700">
                    {s}
                  </span>
                ))}
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <span className="text-xs font-semibold uppercase text-neutral-400">
                Condiciones sugeridas
              </span>
              {selected.results.map((r) => (
                <div key={r.conditionId} className="rounded-lg border border-neutral-100 p-3 flex flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold">{r.condition}</span>
                    <span className="text-sm font-bold text-primary-700">
                      {Math.round(r.probability * 100)}%
                    </span>
                  </div>
                  {r.recommendation && (
                    <p className="text-xs text-neutral-500">{r.recommendation}</p>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {sharingTriage && (
        <ShareTriageModal
          triageId={sharingTriage.id}
          onClose={() => setSharingTriage(null)}
        />
      )}

    </main>
  );
}
