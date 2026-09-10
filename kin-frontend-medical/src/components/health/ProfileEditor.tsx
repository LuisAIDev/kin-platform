"use client";

import { useEffect, useState } from "react";
import {
  RISK_FACTOR_OPTIONS,
  dashboardService,
  type PatientProfile,
} from "@/services/dashboard";

export default function ProfileEditor() {
  const [profile, setProfile] = useState<PatientProfile | null>(null);
  const [riskFactors, setRiskFactors] = useState<Set<string>>(new Set());
  const [chronicConditions, setChronicConditions] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    dashboardService
      .profile()
      .then((data) => {
        if (cancelled) return;
        setProfile(data);
        setRiskFactors(new Set(data.riskFactors));
        setChronicConditions(data.chronicConditions.join(", "));
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const toggleRisk = (factor: string) => {
    setRiskFactors((prev) => {
      const next = new Set(prev);
      if (next.has(factor)) next.delete(factor);
      else next.add(factor);
      return next;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    setMessage("");
    setError("");
    try {
      const conditions = chronicConditions
        .split(",")
        .map((s) => s.trim().toLowerCase())
        .filter(Boolean);
      const saved = await dashboardService.updateProfile(Array.from(riskFactors), conditions);
      setProfile(saved);
      setMessage("Perfil guardado. Se usará en tus próximos diagnósticos.");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  if (!profile) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-500">
        Cargando perfil…
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-6 flex flex-col gap-5">
      <div>
        <h2 className="text-base font-semibold">Factores de riesgo</h2>
        <p className="text-sm text-neutral-500 mt-0.5">
          Selecciona tus factores de riesgo; se usarán para ajustar las
          probabilidades en tus diagnósticos diferenciales.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {RISK_FACTOR_OPTIONS.map((factor) => {
          const isSelected = riskFactors.has(factor);
          return (
            <button
              key={factor}
              type="button"
              onClick={() => toggleRisk(factor)}
              aria-pressed={isSelected}
              className={`rounded-full border px-3 py-1.5 text-sm font-medium transition ${
                isSelected
                  ? "bg-primary-600 border-primary-600 text-white"
                  : "border-neutral-300 text-neutral-700 hover:bg-primary-50 hover:border-primary-400"
              }`}
            >
              {factor}
            </button>
          );
        })}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="chronic-conditions" className="text-sm font-medium">
          Condiciones crónicas
        </label>
        <input
          id="chronic-conditions"
          type="text"
          value={chronicConditions}
          onChange={(e) => setChronicConditions(e.target.value)}
          placeholder="Ej: hipertensión, diabetes"
          className="rounded-lg border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <p className="text-xs text-neutral-400">Separadas por comas.</p>
      </div>

      {message && (
        <p className="text-sm text-emerald-700 bg-emerald-50 px-4 py-2.5 rounded-lg">{message}</p>
      )}
      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
      )}

      <div>
        <button
          type="button"
          onClick={handleSave}
          disabled={saving}
          className="rounded-lg bg-primary-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
        >
          {saving ? "Guardando..." : "Guardar perfil"}
        </button>
      </div>
    </div>
  );
}
