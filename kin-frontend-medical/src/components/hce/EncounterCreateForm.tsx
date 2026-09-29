"use client";

import { useMemo, useState } from "react";
import { hceApi, type CreateEncounterRequest, type EncounterResponse } from "@/lib/hce/api/hce.api";
import type { PhysicianPatientSummary } from "@/services/physician";

const ENCOUNTER_TYPES: { value: string; label: string }[] = [
  { value: "OUTPATIENT", label: "Consulta externa" },
  { value: "INPATIENT", label: "Hospitalización" },
  { value: "EMERGENCY", label: "Urgencias" },
  { value: "TELEMEDICINE", label: "Telemedicina" },
  { value: "HOME_CARE", label: "Atención domiciliaria" },
  { value: "DAY_SURGERY", label: "Cirugía ambulatoria" },
];

/**
 * Formulario para crear un nuevo encuentro clínico (HCE). El médico elige un
 * paciente de su cartera, define el tipo de consulta y el motivo. Al crear,
 * delega la navegación al consumidor vía {@code onCreated} para mantener el
 * componente desacoplado del router y testeable.
 */
export default function EncounterCreateForm({
  patients,
  defaultPatientId,
  onCreated,
}: {
  patients: PhysicianPatientSummary[];
  defaultPatientId?: string;
  onCreated: (encounter: EncounterResponse) => void;
}) {
  const [search, setSearch] = useState("");
  const [patientId, setPatientId] = useState(defaultPatientId ?? "");
  const [encounterType, setEncounterType] = useState("OUTPATIENT");
  const [chiefComplaint, setChiefComplaint] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const filteredPatients = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return patients;
    return patients.filter((p) => p.patientName.toLowerCase().includes(query));
  }, [patients, search]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!patientId) {
      setError("Selecciona un paciente para iniciar la consulta.");
      return;
    }
    if (!chiefComplaint.trim()) {
      setError("El motivo de consulta es obligatorio.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const payload: CreateEncounterRequest = {
        patientId,
        encounterType,
        chiefComplaint: chiefComplaint.trim(),
      };
      const created = await hceApi.createEncounter(payload);
      onCreated(created);
    } catch (err) {
      setError((err as Error).message || "No se pudo crear la consulta.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      aria-label="Nueva consulta"
      className="flex flex-col gap-5 rounded-xl border border-neutral-200 bg-white p-6"
    >
      {patients.length === 0 && (
        <div
          role="status"
          className="text-sm text-neutral-600 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3"
        >
          No tienes pacientes asignados aún. Invita a un paciente primero.
        </div>
      )}

      <div className="flex flex-col gap-1.5">
        <label htmlFor="encounter-patient-search" className="text-sm font-medium">
          Buscar paciente
        </label>
        <input
          id="encounter-patient-search"
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Nombre del paciente..."
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="encounter-patient" className="text-sm font-medium">
          Paciente
        </label>
        <select
          id="encounter-patient"
          required
          value={patientId}
          onChange={(e) => setPatientId(e.target.value)}
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          {patients.length === 0 ? (
            <option value="" disabled>
              No hay pacientes disponibles
            </option>
          ) : (
            <option value="">Selecciona un paciente</option>
          )}
          {filteredPatients.map((p) => (
            <option key={p.patientId} value={p.patientId}>
              {p.patientName}
            </option>
          ))}
        </select>
        {patients.length > 0 && filteredPatients.length === 0 && (
          <p className="text-xs text-neutral-500">No hay pacientes que coincidan con la búsqueda.</p>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="encounter-type" className="text-sm font-medium">
          Tipo de consulta
        </label>
        <select
          id="encounter-type"
          value={encounterType}
          onChange={(e) => setEncounterType(e.target.value)}
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          {ENCOUNTER_TYPES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="encounter-complaint" className="text-sm font-medium">
          Motivo de consulta
        </label>
        <textarea
          id="encounter-complaint"
          required
          rows={3}
          maxLength={500}
          value={chiefComplaint}
          onChange={(e) => setChiefComplaint(e.target.value)}
          placeholder="Describe el motivo de la consulta..."
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
        />
      </div>

      {error && (
        <p role="alert" className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">
          {error}
        </p>
      )}

      <div className="flex justify-end gap-2">
        <button
          type="submit"
          disabled={saving || !patientId || !chiefComplaint.trim()}
          className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300 disabled:cursor-not-allowed"
        >
          {saving ? "Creando..." : "Crear consulta"}
        </button>
      </div>
    </form>
  );
}
