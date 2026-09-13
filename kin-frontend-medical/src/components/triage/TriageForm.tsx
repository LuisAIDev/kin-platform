"use client";

import { useEffect, useMemo, useState } from "react";
import {
  triageService,
  type TriageConditionResult,
  type TriageResponse,
  type TriageSymptom,
} from "@/services/triage";
import DifferentialSection from "@/components/triage/DifferentialSection";
import { TriageDisclaimer } from "@/components/ui/TriageDisclaimer";

const SEVERITY_COLORS: Record<string, string> = {
  LEVE: "bg-emerald-100 text-emerald-800",
  MODERADO: "bg-amber-100 text-amber-800",
  GRAVE: "bg-red-100 text-red-800",
};

const URGENCY_COLORS: Record<string, string> = {
  BAJA: "bg-slate-100 text-slate-700",
  MEDIA: "bg-blue-100 text-blue-800",
  ALTA: "bg-orange-100 text-orange-800",
};

const PLANS_URL = "/dashboard/patient/plans";

export default function TriageForm() {
  const [symptoms, setSymptoms] = useState<TriageSymptom[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [query, setQuery] = useState("");
  const [result, setResult] = useState<TriageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [quotaReached, setQuotaReached] = useState(false);

  useEffect(() => {
    let cancelled = false;
    triageService
      .listSymptoms()
      .then((data) => {
        if (!cancelled) setSymptoms(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return symptoms;
    return symptoms.filter((s) => s.name.toLowerCase().includes(q));
  }, [symptoms, query]);

  const toggle = (name: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  };

  const handleAnalyze = async () => {
    if (selected.size === 0) return;
    setLoading(true);
    setError("");
    setQuotaReached(false);
    try {
      const response = await triageService.analyze(Array.from(selected));
      setResult(response);
    } catch (err) {
      const apiError = err as Error & { code?: string };
      if (apiError.code === "QUOTA_EXCEEDED") {
        setQuotaReached(true);
        setResult(null);
      } else {
        setError(apiError.message);
      }
    } finally {
      setLoading(false);
    }
  };

  if (quotaReached) {
    return (
      <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
        <div className="w-full max-w-2xl flex flex-col gap-4">
          <div className="rounded-xl border border-yellow-200 bg-yellow-50 p-6 flex flex-col gap-4">
            <div>
              <h1 className="text-lg font-bold text-yellow-900">
                Has alcanzado el límite de triajes gratuitos
              </h1>
              <p className="text-sm text-yellow-800 mt-1">
                Contrata el plan Unlimited por $9/mes para continuar realizando
                consultas de triaje sin límite.
              </p>
            </div>
            <a
              href={PLANS_URL}
              className="rounded-lg bg-primary-600 px-4 py-2 text-center text-sm font-medium text-white hover:bg-primary-700 transition"
            >
              Ver planes
            </a>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-3xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Triaje Digital</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Selecciona tus síntomas y obtén una orientación inicial de posibles
            condiciones.
          </p>
        </div>

        <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          ⚠️ Esta herramienta es informativa y de apoyo a la decisión. No
          sustituye la evaluación ni el diagnóstico de un profesional de la
          salud. Ante síntomas graves o persistentes, consulta a un médico.
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">
            {error}
          </p>
        )}

        <div className="flex flex-col gap-2">
          <label htmlFor="symptom-search" className="text-sm font-medium">
            Buscar síntoma
          </label>
          <input
            id="symptom-search"
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Ej: fiebre, tos, dolor de cabeza..."
            className="rounded-lg border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          {filtered.map((s) => {
            const isSelected = selected.has(s.name);
            return (
              <button
                key={s.id}
                type="button"
                onClick={() => toggle(s.name)}
                aria-pressed={isSelected}
                className={`rounded-full border px-4 py-2 text-sm font-medium transition ${
                  isSelected
                    ? "bg-primary-600 border-primary-600 text-white"
                    : "border-neutral-300 text-neutral-700 hover:bg-primary-50 hover:border-primary-400"
                }`}
              >
                {s.name}
              </button>
            );
          })}
          {filtered.length === 0 && (
            <p className="text-sm text-neutral-400">Sin resultados de búsqueda.</p>
          )}
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={handleAnalyze}
            disabled={loading || selected.size === 0}
            className="rounded-lg bg-primary-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
          >
            {loading ? "Analizando..." : `Analizar (${selected.size})`}
          </button>
          {selected.size > 0 && (
            <button
              type="button"
              onClick={() => setSelected(new Set())}
              className="rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-100 transition"
            >
              Limpiar
            </button>
          )}
        </div>

        {result && <Results results={result.results} disclaimer={result.disclaimer} />}

        {result && <DifferentialSection symptoms={Array.from(selected)} />}
      </div>
    </main>
  );
}

function Results({
  results,
  disclaimer,
}: {
  results: TriageConditionResult[];
  disclaimer: string;
}) {
  if (results.length === 0) {
    return (
      <div className="rounded-lg border border-neutral-200 px-5 py-6 text-center text-sm text-neutral-500">
        No encontramos condiciones compatibles con los síntomas seleccionados.
      </div>
    );
  }

  return (
    <section className="flex flex-col gap-4">
      <TriageDisclaimer variant="ui" />
      <h2 className="text-lg font-semibold">Posibles condiciones</h2>
      <div className="flex flex-col gap-4">
        {results.map((r) => (
          <article
            key={r.conditionId}
            className="rounded-xl border border-neutral-200 p-5 flex flex-col gap-3"
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="text-base font-semibold">{r.condition}</h3>
                {r.description && (
                  <p className="text-sm text-neutral-500 mt-0.5">{r.description}</p>
                )}
              </div>
              <span className="text-lg font-bold text-primary-700 shrink-0">
                {Math.round(r.probability * 100)}%
              </span>
            </div>

            <div className="w-full h-2 rounded-full bg-neutral-100 overflow-hidden">
              <div
                className="h-full bg-primary-600"
                style={{ width: `${Math.round(r.probability * 100)}%` }}
              />
            </div>

            <div className="flex flex-wrap gap-2 text-xs">
              <span className={`rounded-full px-2.5 py-1 font-medium ${SEVERITY_COLORS[r.severity]}`}>
                Severidad: {r.severity}
              </span>
              <span className={`rounded-full px-2.5 py-1 font-medium ${URGENCY_COLORS[r.urgency]}`}>
                Urgencia: {r.urgency}
              </span>
            </div>

            {r.recommendation && (
              <p className="text-sm text-neutral-600">{r.recommendation}</p>
            )}

            {r.matchedSymptoms.length > 0 && (
              <p className="text-xs text-neutral-400">
                Síntomas coincidentes: {r.matchedSymptoms.join(", ")}
              </p>
            )}
          </article>
        ))}
      </div>
      <p className="text-xs text-neutral-400">{disclaimer}</p>
    </section>
  );
}
