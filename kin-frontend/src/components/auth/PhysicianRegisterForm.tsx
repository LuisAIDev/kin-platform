"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { PasswordInput } from "@/components/auth/PasswordInput";
import ConsentCheckbox from "@/components/auth/ConsentCheckbox";

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

/**
 * Formulario de auto-registro de médico (vertical Salud). La cuenta queda
 * pendiente de verificación por un administrador (cédula profesional).
 */
export default function PhysicianRegisterForm() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [licenseNumber, setLicenseNumber] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [country, setCountry] = useState("");
  const [phone, setPhone] = useState("");
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const result = await authService.registerPhysician({
        fullName,
        email,
        password,
        licenseNumber,
        specialty,
        country,
        phone: phone || undefined,
        healthDataConsent: consent,
      });
      if (result.error) {
        setError(result.error);
        return;
      }
      const state = result.data?.state;
      switch (state) {
        case "ACCOUNT_ALREADY_VERIFIED":
          setError(
            "Esta cuenta ya está verificada. Inicia sesión para solicitar el registro como profesional.",
          );
          return;
        case "ACCOUNT_NOT_VERIFIED":
          setError("Debes verificar primero tu correo. Revisa tu bandeja de entrada.");
          return;
        case "PHYSICIAN_PENDING":
          setError("Tu solicitud profesional está pendiente de revisión.");
          return;
        case "PHYSICIAN_APPROVED":
          setError("Esta cuenta ya está habilitada como profesional.");
          return;
        case "PHYSICIAN_REJECTED":
          setError("Tu solicitud fue rechazada. Puedes intentarlo de nuevo.");
          return;
        default:
          // NEW_REGISTRATION (o estado desconocido): se envió correo de verificación.
          router.push(`/verify-email?email=${encodeURIComponent(email)}&pending=1`);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{error}</p>
      )}

      <p className="text-xs text-neutral-500 bg-neutral-50 border border-neutral-200 px-3 py-2 rounded-lg">
        Tu cuenta quedará <strong>pendiente de verificación</strong>: un
        administrador revisará tu cédula profesional antes de habilitar el acceso.
      </p>

      <input
        type="text"
        placeholder="Nombre y apellidos"
        value={fullName}
        onChange={(e) => setFullName(e.target.value)}
        required
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
      />

      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
      />

      <PasswordInput
        value={password}
        onChange={setPassword}
        placeholder="Contraseña (mín. 8 caracteres)"
        minLength={8}
        autoComplete="new-password"
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 w-full pr-10"
      />

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

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-xs text-neutral-500">
          Especialidad
          <select
            value={specialty}
            onChange={(e) => setSpecialty(e.target.value)}
            required
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 bg-white"
          >
            <option value="">Selecciona</option>
            {SPECIALTIES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-xs text-neutral-500">
          País
          <input
            type="text"
            placeholder="Ej. México"
            value={country}
            onChange={(e) => setCountry(e.target.value)}
            required
            maxLength={60}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
          />
        </label>
      </div>

      <input
        type="tel"
        placeholder="Teléfono (opcional)"
        value={phone}
        onChange={(e) => setPhone(e.target.value)}
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
      />

      <ConsentCheckbox
        checked={consent}
        onChange={setConsent}
        text="Acepto los términos de uso de KIN Platform para profesionales de salud y confirmo que la información proporcionada (incluyendo mi cédula profesional) es veraz, conforme a la política de privacidad de KIN."
      />

      <button
        type="submit"
        disabled={loading}
        className="rounded-lg bg-emerald-600 py-2 text-sm font-medium text-white hover:bg-emerald-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {loading ? "Enviando solicitud..." : "Solicitar registro como médico"}
      </button>
    </form>
  );
}
