"use client";

import { useEffect, useState } from "react";
import CarePlanView from "@/components/health/CarePlanView";
import FeedbackButton from "@/components/health/FeedbackButton";
import HealthSummaryCards from "@/components/health/HealthSummaryCards";
import HistoryList from "@/components/health/HistoryList";
import ProfileEditor from "@/components/health/ProfileEditor";
import SymptomEvolutionChart from "@/components/health/SymptomEvolutionChart";
import { dashboardService } from "@/services/dashboard";
import type { CarePlan, HealthSummary } from "@/services/dashboard";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "@/services/triage";

export default function PatientHealthPage() {
  const [summary, setSummary] = useState<HealthSummary | null>(null);
  const [carePlan, setCarePlan] = useState<CarePlan | null>(null);
  const [page, setPage] = useState<PageResponse<TriageHistoryEntry> | null>(null);
  const [history, setHistory] = useState<TriageHistoryEntry[]>([]);
  const [selected, setSelected] = useState<TriageHistoryEntry | null>(null);
  const [error, setError] = useState("");

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
    try {
      const historyData = await dashboardService.history(pageNumber, 10);
      setPage(historyData);
    } catch (err) {
      setError((err as Error).message);
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

        <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          ⚠️ Esta información es de apoyo informativo. No sustituye la
          evaluación ni el diagnóstico de un profesional de la salud.
        </div>

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
    </main>
  );
}
