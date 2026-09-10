"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { PasswordInput } from "@/components/auth/PasswordInput";
import ConsentCheckbox from "@/components/auth/ConsentCheckbox";

const SEX_OPTIONS = ["FEMENINO", "MASCULINO", "OTRO"];

/**
 * Formulario de auto-registro de paciente (vertical Salud). Reutilizable.
 */
export default function PatientRegisterForm() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [sex, setSex] = useState("");
  const [phone, setPhone] = useState("");
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const result = await authService.registerPatient({
        fullName,
        email,
        password,
        dateOfBirth,
        sex,
        phone: phone || undefined,
        healthDataConsent: consent,
      });
      if (result.error) {
        setError(result.error);
        return;
      }
      router.push(`/verify-email?email=${encodeURIComponent(email)}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{error}</p>
      )}

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

      <div className="grid grid-cols-2 gap-3">
        <label className="flex flex-col gap-1 text-xs text-neutral-500">
          Fecha de nacimiento
          <input
            type="date"
            value={dateOfBirth}
            onChange={(e) => setDateOfBirth(e.target.value)}
            required
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
          />
        </label>
        <label className="flex flex-col gap-1 text-xs text-neutral-500">
          Sexo
          <select
            value={sex}
            onChange={(e) => setSex(e.target.value)}
            required
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 bg-white"
          >
            <option value="">Selecciona</option>
            {SEX_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
      </div>

      <input
        type="tel"
        placeholder="Teléfono (opcional)"
        value={phone}
        onChange={(e) => setPhone(e.target.value)}
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
      />

      <ConsentCheckbox checked={consent} onChange={setConsent} />

      <button
        type="submit"
        disabled={loading}
        className="rounded-lg bg-emerald-600 py-2 text-sm font-medium text-white hover:bg-emerald-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {loading ? "Creando cuenta..." : "Crear cuenta de paciente"}
      </button>
    </form>
  );
}
