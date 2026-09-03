"use client";

import { useEffect, useMemo, useState } from "react";
import {
  triageService,
  type TriageConditionResult,
  type TriageResponse,
  type TriageSymptom,
} from "@/services/triage";
import DifferentialSection from "@/components/triage/DifferentialSection";
import { usePathname, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/dialog";
import { useTranslation } from "next-i18next";

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

type QuotaExceededError = {
  code: "QUOTA_EXCEEDED";
  message: string;
  redirectUrl: string;
};

export default function TriageForm() {
  const [symptoms, setSymptoms] = useState<TriageSymptom[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [query, setQuery] = useState("");
  const [result, setResult] = useState<TriageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [quotaError, setQuotaError] = useState<QuotaExceededError | null>(null);
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    // Verificar si hay un error de quota en la URL o estado
    const handleError = (err: any) => {
      if (err?.code === "QUOTA_EXCEEDED") {
        setQuotaError({
          code: "QUOTA_EXCEEDED",
          message: err.message,
          redirectUrl: err.redirectUrl || "/dashboard/patient/plans",
        });
      } else {
        setError(err?.message || "Error inesperado");
      }
    };

    triageService
      .listSymptoms()
      .then((data) => {
        // Síntomas cargados exitosamente, limpiar cualquier error anterior
        if (quotaError?.code) {
          setQuotaError(null);
        }
        setSymptoms(data);
      }
      .catch(handleError);
    }, [pathname, quotaError]);

  // Efecto para manejar el error de quota cuando cambia
  useEffect(() => {
    if (quotaError?.code === "QUOTA_EXCEEDED") {
      // Redirigir a la página de planes después de un breve delay
      const timeoutId = setTimeout(() => {
        window.location.href = quotaError.redirectUrl;
      }, 3000);
      return () => clearTimeout(timeoutId);
    }
  }, [quotaError]);

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
    setQuotaError(null);
    try {
      const response = await triageService.analyze(Array.from(selected));
      setResult(response);
    } catch (err) {
      handleAnalyzeError(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyzeError = (err: any) => {
    if (err?.code === "QUOTA_EXCEEDED") {
      setQuotaError({
        code: "QUOTA_EXCEEDED",
        message: err.message,
        redirectUrl: err.redirectUrl || "/dashboard/patient/plans",
      });
    } else {
      setError(err?.message || "Error inesperado");
    }
  };

  const toggle = (name: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-3xl flex flex-col gap-6">
        {/* Mostrar error de quota si existe */}
        {quotaError && (
          <div className="mb-6 p-4 rounded-lg bg-yellow-50 border border-yellow-200">
            <AlertDialog>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>
                    Límite de triajes alcanzado
                  </AlertDialogTitle>
                </AlertDialogHeader>
                <AlertDialogDescription>
                  {quotaError.message}
                </AlertDialogDescription>
              </AlertDialogContent>
              <AlertDialogFooter>
                <AlertDialogAction
                  onClick={() => {
                    window.location.href = quotaError.redirectUrl;
                  }}
                >
                  Ver planes
                </AlertDialogAction>
                <AlertDialogCancel>Cancelar</AlertDialogCancel>
              </AlertDialogFooter>
            </AlertDialog>
          </div>
        )}

        {!quotaError && (
          <div>
            <h1 className="text-2xl font-bold">Triaje Digital</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Selecciona tus síntomas y obtén una orientación inicial de posibles
              condiciones.
            </p>
          </div>
        )}

        {error && !quotaError && (
          <div className="mb-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-800">
            <p className="text-sm">{error}</p>
          </div>
        )}

        <Card className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          ⚠️ Esta herramienta es informativa y de apoyo a la decisión. No
          sustituye la evaluación ni el diagnóstico de un profesional de la
          salud. Ante síntomas graves o persistentes, consulta a un médico.
        </div>

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

        {result && <DifferentialSection symptoms={Array.from(selected)} />}
      </div>
    </main>
  );
}