"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { physicianService, type PhysicianApplicationStatus } from "@/services/physician";
import { authService } from "@/services/auth";
import { hasPhysicianCapability, isAdminRole } from "@/utils/roles";

const SPECIALTIES = [
  "Medicina General",
  "Medicina Interna",
  "Pediatría",
  "Cardiología",
  "Dermatología",
  "Ginecología y Obstetricia",
  "Traumatología",
  "Neumología",
  "Neurología",
  "Psiquiatría",
  "Otras",
];

type Status = "loading" | "form" | "success";

/**
 * Widget de solicitud de capacidad profesional (médico) para cuentas existentes.
 *
 * - Sin solicitud: ofrece el formulario (cédula, especialidad, país, teléfono,
 *   consentimiento) → POST /health/physician/application.
 * - PENDING: banner informativo (no bloquea la cuenta persona).
 * - REJECTED: banner con opción de volver a solicitar.
 * - APPROVED / ya médico: no muestra nada (el acceso al portal se gestiona vía
 *   physicianCapability / menú lateral).
 */
export default function PhysicianApplicationWidget() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const pathname = usePathname();
  const [appStatus, setAppStatus] = useState<PhysicianApplicationStatus | null>(null);
  const [ui, setUi] = useState<Status>("loading");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const [licenseNumber, setLicenseNumber] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [country, setCountry] = useState("");
  const [phone, setPhone] = useState("");
  const [consent, setConsent] = useState(false);

  // El formulario de solicitud profesional (médico) NO debe aparecer en el
  // módulo de paciente: allí el usuario opera como paciente (ver/aceptar
  // invitaciones de médicos). La solicitud de capacidad profesional solo se
  // ofrece en los hubs generales de persona (empresa/salud), no en /dashboard/patient/**.
  const onPatientModule = pathname?.startsWith("/dashboard/patient") ?? false;

  useEffect(() => {
    if (onPatientModule) return;
    let cancelled = false;
    async function load() {
      // ADMIN y quien ya tiene capacidad profesional no necesitan este widget.
      if (!user || isAdminRole(user.role) || hasPhysicianCapability(user)) {
        return;
      }
      try {
        const st = await physicianService.applicationStatus();
        if (cancelled) return;
        setAppStatus(st);
        setUi(st.status === "NOT_FOUND" ? "form" : "success");
      } catch {
        // Sin red o error transitorio: no mostrar el widget (el hub sigue operativo).
        if (!cancelled) setUi("loading");
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [user, onPatientModule]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await physicianService.applyAsPhysician({
        licenseNumber,
        specialty,
        country,
        phone: phone || undefined,
        healthDataConsent: consent,
      });
      // La solicitud quedó en PENDING: reflejarlo de inmediato (sin depender de
      // una re-consulta que podría tardar).
      setAppStatus({
        status: "PENDING",
        role: user?.role ?? null,
        licenseNumber,
        specialty,
        country,
        phone: phone || null,
        physicianVerificationStatus: "PENDING",
      });
      setUi("success");
    } catch (err) {
      setError((err as Error).message || "No se pudo enviar la solicitud. Inténtalo de nuevo.");
    } finally {
      setSubmitting(false);
    }
  };

  // Nunca en el módulo de paciente (el widget es de capacidad profesional).
  if (onPatientModule) return null;

  // Oculto mientras carga o para ADMIN/PHYSICIAN (no aplican a este widget).
  if (ui === "loading") return null;

  // Banner informativo de solicitud pendiente.
  if (ui === "success" && appStatus?.status === "PENDING") {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 mb-4">
        <p className="text-sm font-medium text-amber-800">
          Tu solicitud profesional está pendiente de revisión.
        </p>
        <p className="text-xs text-amber-700 mt-1">
          Un administrador revisará tu cédula profesional. Mientras tanto puedes seguir usando tu
          cuenta con normalidad.
        </p>
      </div>
    );
  }

  // Si ya está habilitado como profesional, solo mostrar un aviso con acceso al portal.
  if (ui === "success" && appStatus?.status === "APPROVED") {
    return (
      <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-5 py-4 mb-4">
        <p className="text-sm font-medium text-emerald-800">
          Esta cuenta ya está habilitada como profesional.{" "}
          <Link href="/dashboard/physician" className="underline font-medium">
            Ir al portal médico
          </Link>
        </p>
      </div>
    );
  }

  // Banner de rechazo (solo si no estamos re-solicitando).
  if (appStatus?.status === "REJECTED" && ui !== "form") {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-5 py-4 mb-4">
        <p className="text-sm font-medium text-red-800">Tu solicitud profesional fue rechazada.</p>
        <button
          onClick={() => setUi("form")}
          className="mt-2 rounded-lg border border-red-300 px-4 py-1.5 text-xs font-medium text-red-700 hover:bg-red-100 transition"
        >
          Volver a solicitar
        </button>
      </div>
    );
  }
  if (ui !== "form") return null;

  return (
    <div className="rounded-xl border border-primary-200 bg-white px-5 py-4 mb-4">
      <div className="flex flex-col gap-1">
        <h2 className="text-base font-bold text-neutral-900">Solicitar registro como profesional</h2>
        <p className="text-xs text-neutral-500">
          ¿Eres profesional de salud? Solicita acceso al portal médico. Un administrador revisará tu
          cédula profesional antes de habilitar el acceso.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-3 mt-3">
        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{error}</p>
        )}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <input
            type="text"
            placeholder="Número de cédula profesional"
            value={licenseNumber}
            onChange={(e) => setLicenseNumber(e.target.value)}
            required
            minLength={4}
            maxLength={20}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
          />
          <select
            value={specialty}
            onChange={(e) => setSpecialty(e.target.value)}
            required
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 bg-white"
          >
            <option value="">Especialidad</option>
            {SPECIALTIES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <input
            type="text"
            placeholder="País (ej. México)"
            value={country}
            onChange={(e) => setCountry(e.target.value)}
            required
            maxLength={60}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
          />
          <input
            type="tel"
            placeholder="Teléfono (opcional)"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            maxLength={30}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
          />
        </div>
        <label className="flex items-start gap-2 text-xs text-neutral-600">
          <input
            type="checkbox"
            checked={consent}
            onChange={(e) => setConsent(e.target.checked)}
            required
            className="mt-0.5"
          />
          <span>
            Acepto los términos de uso de KIN Platform para profesionales de salud y confirmo que la
            información proporcionada (incluyendo mi cédula profesional) es veraz.
          </span>
        </label>
        <button
          type="submit"
          disabled={submitting}
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? "Enviando..." : "Enviar solicitud"}
        </button>
      </form>
    </div>
  );
}
