"use client";

import { useState } from "react";
import { telemedicineService } from "@/services/telemedicine";

export default function AppointmentForm({
  physicianId,
  onCreated,
}: {
  physicianId?: string;
  onCreated: () => void;
}) {
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!date || !time || !reason.trim()) return;
    setSaving(true);
    setMessage("");
    setError("");
    try {
      const scheduledAt = new Date(`${date}T${time}:00`).toISOString();
      if (physicianId) {
        await telemedicineService.requestAppointment(physicianId, scheduledAt, reason.trim());
      } else {
        // Sin médico específico: el frontend del paciente elige de su lista
        throw new Error("Se requiere seleccionar un médico.");
      }
      setMessage("Cita solicitada. Espera la confirmación del médico.");
      setDate("");
      setTime("");
      setReason("");
      onCreated();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-6 flex flex-col gap-4">
      <h2 className="text-base font-semibold">Solicitar cita</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="appt-date" className="text-sm font-medium">
            Fecha
          </label>
          <input
            id="appt-date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="appt-time" className="text-sm font-medium">
            Hora
          </label>
          <input
            id="appt-time"
            type="time"
            value={time}
            onChange={(e) => setTime(e.target.value)}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="appt-reason" className="text-sm font-medium">
          Motivo
        </label>
        <textarea
          id="appt-reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          rows={3}
          maxLength={500}
          placeholder="Describe brevemente el motivo de la cita..."
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
        />
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
          onClick={handleSubmit}
          disabled={saving || !date || !time || !reason.trim()}
          className="rounded-lg bg-primary-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
        >
          {saving ? "Solicitando..." : "Solicitar cita"}
        </button>
      </div>
    </div>
  );
}
