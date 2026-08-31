"use client";

import { useEffect, useState } from "react";
import { patientRelationshipService } from "@/services/patient";
import type { PendingInvitation } from "@/services/patient";

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

/**
 * Lista de invitaciones pendientes del paciente. Permite aceptar o rechazar
 * la invitación de un médico (ciclo de vida de relación V30). Tras aceptar, la
 * relación queda ACTIVE y el médico aparece en la mensajería/citas.
 */
export default function InvitationsList() {
  const [invitations, setInvitations] = useState<PendingInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const load = async () => {
    try {
      const data = await patientRelationshipService.pendingInvitations();
      setInvitations(data);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    patientRelationshipService
      .pendingInvitations()
      .then((data) => {
        if (!cancelled) setInvitations(data);
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

  const handleAccept = async (physicianId: string, physicianName: string) => {
    setProcessing(physicianId);
    setError("");
    setMessage("");
    try {
      await patientRelationshipService.acceptInvitation(physicianId);
      setMessage(`Has aceptado la invitación de ${physicianName}. Ya puedes comunicarte con tu médico.`);
      await load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setProcessing(null);
    }
  };

  const handleReject = async (physicianId: string, physicianName: string) => {
    setProcessing(physicianId);
    setError("");
    setMessage("");
    try {
      await patientRelationshipService.rejectInvitation(physicianId);
      setMessage(`Has rechazado la invitación de ${physicianName}.`);
      await load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setProcessing(null);
    }
  };

  if (loading) {
    return <p className="text-sm text-neutral-500">Cargando invitaciones...</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      {message && (
        <p className="text-sm text-emerald-700 bg-emerald-50 px-4 py-2.5 rounded-lg">{message}</p>
      )}
      {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

      {invitations.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-300 p-8 text-center text-sm text-neutral-500">
          No tienes invitaciones pendientes de médicos.
        </div>
      ) : (
        invitations.map((inv) => (
          <div
            key={inv.physicianId}
            className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col sm:flex-row sm:items-center gap-4"
          >
            <div className="flex-1 flex flex-col gap-0.5">
              <p className="font-semibold text-neutral-800">{inv.physicianName}</p>
              {inv.specialty && (
                <p className="text-sm text-neutral-500">
                  Especialidad: <span className="text-neutral-700">{inv.specialty}</span>
                </p>
              )}
              <p className="text-xs text-neutral-400">
                Invitación recibida el {formatDate(inv.invitedAt)}
              </p>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handleAccept(inv.physicianId, inv.physicianName)}
                disabled={processing === inv.physicianId}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
              >
                {processing === inv.physicianId ? "Procesando..." : "Aceptar"}
              </button>
              <button
                type="button"
                onClick={() => handleReject(inv.physicianId, inv.physicianName)}
                disabled={processing === inv.physicianId}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 transition disabled:opacity-40"
              >
                Rechazar
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
