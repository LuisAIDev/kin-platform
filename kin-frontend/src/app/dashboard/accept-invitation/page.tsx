"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { patientRelationshipService } from "@/services/patient";

type FlowState =
  | { kind: "loading" }
  | { kind: "success" }
  | { kind: "error"; message: string };

/**
 * Página de destino del enlace del correo de invitación cuando el paciente aún
 * no tiene capacidad de paciente (falta consentimiento de datos de salud). Al
 * cargar, acepta el consentimiento y vincula la invitación PENDING_CONSENT con
 * el médico (→ ACTIVE) en un solo clic.
 */
export default function AcceptInvitationPage() {
  const [state, setState] = useState<FlowState>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const physicianId = new URLSearchParams(window.location.search).get("physicianId");
      if (!physicianId) {
        await Promise.resolve();
        if (!cancelled) {
          setState({ kind: "error", message: "Falta el identificador del médico en el enlace." });
        }
        return;
      }
      try {
        await patientRelationshipService.acceptConsentAndLink(physicianId);
        if (!cancelled) setState({ kind: "success" });
      } catch {
        if (!cancelled) {
          setState({
            kind: "error",
            message:
              "No se pudo completar la vinculación. Revisa que hayas iniciado sesión con la cuenta invitada.",
          });
        }
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="flex-1 px-6 py-12 max-w-xl mx-auto w-full">
      {state.kind === "loading" && (
        <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center">
          <p className="text-sm text-neutral-500">Procesando tu aceptación del consentimiento...</p>
        </div>
      )}

      {state.kind === "success" && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-8">
          <h1 className="text-xl font-bold text-emerald-900">¡Listo! Te has vinculado con tu médico.</h1>
          <p className="mt-2 text-sm text-emerald-800">
            Aceptaste el consentimiento de tratamiento de datos de salud y la relación
            con tu médico quedó activa. Ya pueden verse, escribirse y gestionar citas.
          </p>
          <a
            href="https://kin-platform-medical.com/dashboard/patient/health"
            className="mt-6 inline-flex rounded-lg bg-primary-600 px-5 py-2 text-sm font-semibold text-white hover:bg-primary-500"
          >
            Ir a Mi Salud
          </a>
        </div>
      )}

      {state.kind === "error" && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-8">
          <h1 className="text-xl font-bold text-red-900">No se pudo completar la vinculación</h1>
          <p className="mt-2 text-sm text-red-800">{state.message}</p>
          <p className="mt-3 text-sm text-red-700">
            Si no has iniciado sesión con la cuenta invitada,{" "}
            <Link href="/login" className="underline font-medium">
              inicia sesión aquí
            </Link>{" "}
            y vuelve a abrir el enlace del correo.
          </p>
        </div>
      )}
    </main>
  );
}
