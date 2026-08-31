"use client";

import { useState } from "react";
import { followUpService } from "@/services/followup";

/**
 * Formulario del médico para registrar la evolución de un paciente
 * (síntomas, signos vitales, adherencia y notas).
 */
export default function EvolutionForm({
  patientId,
  onRecorded,
}: {
  patientId: string;
  onRecorded: () => void;
}) {
  const [symptoms, setSymptoms] = useState("");
  const [bloodPressure, setBloodPressure] = useState("");
  const [weight, setWeight] = useState("");
  const [heartRate, setHeartRate] = useState("");
  const [medicationAdherence, setMedicationAdherence] = useState(true);
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!symptoms.trim() && !notes.trim()) return;
    setSaving(true);
    setError("");
    try {
      const vitals: Record<string, unknown> = {};
      if (bloodPressure.trim()) vitals.presion = bloodPressure.trim();
      if (weight.trim()) vitals.peso = weight.trim();
      if (heartRate.trim()) vitals["frecuencia cardiaca"] = heartRate.trim();
      await followUpService.recordEvolution(patientId, symptoms.trim(), vitals, medicationAdherence, notes.trim());
      setSymptoms("");
      setBloodPressure("");
      setWeight("");
      setHeartRate("");
      setNotes("");
      onRecorded();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
      <h3 className="text-sm font-semibold uppercase text-neutral-400">Registrar evolución</h3>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="evo-symptoms" className="text-sm font-medium">
          Síntomas
        </label>
        <textarea
          id="evo-symptoms"
          value={symptoms}
          onChange={(e) => setSymptoms(e.target.value)}
          rows={2}
          placeholder="Describe la evolución de los síntomas..."
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
        {[
          { key: "evo-pressure", label: "Presión arterial", value: bloodPressure, set: setBloodPressure, placeholder: "120/80" },
          { key: "evo-weight", label: "Peso (kg)", value: weight, set: setWeight, placeholder: "70" },
          { key: "evo-heartrate", label: "Frecuencia cardíaca", value: heartRate, set: setHeartRate, placeholder: "72" },
        ].map((field) => (
          <div key={field.key} className="flex flex-col gap-1.5">
            <label htmlFor={field.key} className="text-xs font-medium">
              {field.label}
            </label>
            <input
              id={field.key}
              type="text"
              value={field.value}
              onChange={(e) => field.set(e.target.value)}
              placeholder={field.placeholder}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        ))}
      </div>

      <div className="flex items-center gap-2">
        <input
          id="evo-adherence"
          type="checkbox"
          checked={medicationAdherence}
          onChange={(e) => setMedicationAdherence(e.target.checked)}
          className="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
        />
        <label htmlFor="evo-adherence" className="text-sm text-neutral-700">
          Cumple la medicación
        </label>
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="evo-notes" className="text-sm font-medium">
          Notas
        </label>
        <textarea
          id="evo-notes"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          rows={2}
          placeholder="Observaciones adicionales..."
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
        />
      </div>

      {error && <p className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

      <button
        type="button"
        onClick={handleSubmit}
        disabled={saving || (!symptoms.trim() && !notes.trim())}
        className="self-start rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
      >
        {saving ? "Guardando..." : "Registrar evolución"}
      </button>
    </div>
  );
}
