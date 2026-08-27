"use client";

import { useEffect, useState } from "react";
import { adminUsersService, type PendingPhysician } from "@/services/adminUsers";

/**
 * Panel de administración: verificación de identidad de médicos auto-registrados
 * (cédula profesional). Aprobar habilita el login; rechazar lo bloquea.
 */
export default function AdminPhysiciansPage() {
  const [physicians, setPhysicians] = useState<PendingPhysician[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);

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

  const handleDecision = async (physician: PendingPhysician, approve: boolean) => {
    setBusyId(physician.id);
    setError("");
    try {
      if (approve) {
        await adminUsersService.approvePhysician(physician.id);
      } else {
        await adminUsersService.rejectPhysician(physician.id);
      }
      setPhysicians((prev) => prev.filter((p) => p.id !== physician.id));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <main className="flex-1 px-6 py-8 max-w-5xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Verificación de médicos</h1>
        <p className="text-sm text-neutral-500">
          Médicos auto-registrados pendientes de validación de su cédula profesional.
        </p>
      </div>

      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg mt-4">{error}</p>
      )}

      {loading ? (
        <p className="text-sm text-neutral-500 mt-6">Cargando solicitudes...</p>
      ) : physicians.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-300 p-10 text-center mt-6">
          <p className="text-sm text-neutral-500">No hay médicos pendientes de verificación.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4 mt-6">
          {physicians.map((physician) => (
            <div
              key={physician.id}
              className="flex flex-col gap-4 rounded-xl border border-neutral-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="flex flex-col gap-1">
                <p className="font-semibold text-neutral-900">{physician.fullName}</p>
                <p className="text-sm text-neutral-500">{physician.email}</p>
                <div className="flex flex-wrap gap-2 mt-1 text-xs">
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
              <div className="flex gap-2 shrink-0">
                <button
                  onClick={() => handleDecision(physician, false)}
                  disabled={busyId === physician.id}
                  className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-50 min-h-10"
                >
                  Rechazar
                </button>
                <button
                  onClick={() => handleDecision(physician, true)}
                  disabled={busyId === physician.id}
                  className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 transition disabled:opacity-50 min-h-10"
                >
                  Aprobar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
