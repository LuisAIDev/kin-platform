"use client";

import { useEffect, useState } from "react";
import { adminUsersService, type PendingPhysician } from "@/services/adminUsers";

/**
 * Panel de administración: revisión de solicitudes de capacidad profesional
 * (médicos). Muestra el rol ORIGINAL del solicitante (FREE/PREMIUM/PATIENT/
 * PHYSICIAN) porque la capacidad es cross-rol (Alternativa B). Aprobar habilita
 * la capacidad; rechazar permite indicar un motivo (se guarda en audit_logs).
 */
export default function AdminPhysiciansPage() {
  const [physicians, setPhysicians] = useState<PendingPhysician[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [reason, setReason] = useState("");

  useEffect(() => {
    let cancelled = false;
    adminUsersService
      .pendingPhysicians()
      .then((list) => {
        if (!cancelled) setPhysicians(list);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleApprove = async (physician: PendingPhysician) => {
    setBusyId(physician.id);
    setError("");
    try {
      await adminUsersService.approvePhysician(physician.id);
      setPhysicians((prev) => prev.filter((p) => p.id !== physician.id));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusyId(null);
    }
  };

  const handleReject = async (physician: PendingPhysician) => {
    setBusyId(physician.id);
    setError("");
    try {
      await adminUsersService.rejectPhysician(physician.id, reason.trim() || undefined);
      setPhysicians((prev) => prev.filter((p) => p.id !== physician.id));
      setRejectingId(null);
      setReason("");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Verificación de profesionales</h1>
        <p className="text-sm text-neutral-500">
          Solicitudes de capacidad profesional pendientes de validación de la cédula (cualquier
          rol: médico nuevo, paciente, empresario, …).
        </p>
      </div>

      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg mt-4">{error}</p>
      )}

      {loading ? (
        <p className="text-sm text-neutral-500 mt-6">Cargando solicitudes...</p>
      ) : physicians.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-300 p-10 text-center mt-6">
          <p className="text-sm text-neutral-500">No hay solicitudes pendientes de verificación.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4 mt-6">
          {physicians.map((physician) => (
            <div
              key={physician.id}
              className="flex flex-col gap-4 rounded-xl border border-neutral-200 bg-white p-5"
            >
              <div className="sm:flex sm:items-start sm:justify-between gap-4">
                <div className="flex flex-col gap-1">
                  <p className="font-semibold text-neutral-900">{physician.fullName}</p>
                  <p className="text-sm text-neutral-500">{physician.email}</p>
                  <div className="flex flex-wrap gap-2 mt-1 text-xs">
                    <span className="rounded-full bg-primary-50 text-primary-700 px-2.5 py-1 font-medium">
                      Rol actual: {physician.role ?? "—"}
                    </span>
                    <span className="rounded-full bg-neutral-100 px-2.5 py-1 text-neutral-600">
                      Cédula: {physician.licenseNumber ?? "—"}
                    </span>
                    <span className="rounded-full bg-neutral-100 px-2.5 py-1 text-neutral-600">
                      {physician.specialty ?? "Sin especialidad"}
                    </span>
                    <span className="rounded-full bg-neutral-100 px-2.5 py-1 text-neutral-600">
                      {physician.country ?? "—"}
                    </span>
                  </div>
                </div>
                <div className="flex gap-2 shrink-0 mt-3 sm:mt-0">
                  <button
                    onClick={() => {
                      setRejectingId(rejectingId === physician.id ? null : physician.id);
                      setReason("");
                    }}
                    disabled={busyId === physician.id}
                    className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-50 min-h-10"
                  >
                    Rechazar
                  </button>
                  <button
                    onClick={() => handleApprove(physician)}
                    disabled={busyId === physician.id}
                    className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 transition disabled:opacity-50 min-h-10"
                  >
                    Aprobar
                  </button>
                </div>
              </div>

              {rejectingId === physician.id && (
                <div className="flex flex-col gap-2 border-t border-neutral-100 pt-3">
                  <label className="text-xs text-neutral-500">
                    Motivo del rechazo (opcional, se guarda en auditoría)
                    <textarea
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      rows={2}
                      maxLength={500}
                      className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                    />
                  </label>
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => setRejectingId(null)}
                      className="rounded-lg px-4 py-2 text-sm text-neutral-500 hover:bg-neutral-50 transition"
                    >
                      Cancelar
                    </button>
                    <button
                      onClick={() => handleReject(physician)}
                      disabled={busyId === physician.id}
                      className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition disabled:opacity-50 min-h-10"
                    >
                      Confirmar rechazo
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
