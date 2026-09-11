"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";
import { PasswordInput } from "@/components/ui/PasswordInput";

export default function PhysicianRegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    licenseNumber: "",
    specialty: "",
    country: "",
    phone: "",
    healthDataConsent: false,
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const target = e.target as HTMLInputElement;
    setForm({
      ...form,
      [target.name]: target.type === "checkbox" ? target.checked : target.value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    const { error } = await authService.registerPhysician({
      fullName: form.fullName,
      email: form.email,
      password: form.password,
      licenseNumber: form.licenseNumber,
      specialty: form.specialty,
      country: form.country,
      phone: form.phone,
      healthDataConsent: form.healthDataConsent,
    });
    setLoading(false);
    if (error) {
      setError(error);
      return;
    }
    router.push("/auth/verify-email");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-medical-50 via-white to-accent-50">
      <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="rounded-2xl shadow-xl border border-neutral-200 bg-white p-8 sm:p-10">
          <div className="text-center mb-8">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-medical-500 to-medical-600">
              <svg className="h-7 w-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-neutral-900">Registro de Médico</h1>
            <p className="text-neutral-500 mt-2">Solicita tu acceso profesional a KIN Medical</p>
          </div>

          {error && (
            <div className="mb-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm" role="alert">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="fullName" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Nombre completo
                </label>
                <input id="fullName" name="fullName" type="text" required value={form.fullName} onChange={handleChange} className="input-field" />
              </div>
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Correo electrónico
                </label>
                <input id="email" name="email" type="email" autoComplete="email" required value={form.email} onChange={handleChange} className="input-field" />
              </div>
            </div>

                        <PasswordInput
              id="password"
              name="password"
              label="Contraseña"
              autoComplete="new-password"
              required
              minLength={8}
              value={form.password}
              onChange={handleChange}
              placeholder="••••••••"
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="licenseNumber" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Nº de cédula profesional
                </label>
                <input id="licenseNumber" name="licenseNumber" type="text" required value={form.licenseNumber} onChange={handleChange} className="input-field" />
              </div>
              <div>
                <label htmlFor="specialty" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Especialidad
                </label>
                <input id="specialty" name="specialty" type="text" required value={form.specialty} onChange={handleChange} className="input-field" />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="country" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  País
                </label>
                <select id="country" name="country" required value={form.country} onChange={handleChange} className="input-field">
                  <option value="">Selecciona un país</option>
                  <option value="ES">España</option>
                  <option value="MX">México</option>
                  <option value="CO">Colombia</option>
                  <option value="AR">Argentina</option>
                  <option value="CL">Chile</option>
                  <option value="PE">Perú</option>
                  <option value="OT">Otro</option>
                </select>
              </div>
              <div>
                <label htmlFor="phone" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Teléfono (opcional)
                </label>
                <input id="phone" name="phone" type="tel" value={form.phone} onChange={handleChange} className="input-field" placeholder="+34 600 000 000" />
              </div>
            </div>

            <label className="flex items-start gap-2 cursor-pointer">
              <input
                name="healthDataConsent"
                type="checkbox"
                required
                checked={form.healthDataConsent}
                onChange={handleChange}
                className="mt-0.5 w-4 h-4 text-medical-600 border-neutral-300 rounded focus:ring-2 focus:ring-medical-500"
              />
              <span className="text-sm text-neutral-600">
                Acepto el tratamiento de mis datos de salud según la{" "}
                <a href="/privacidad" className="text-medical-600 hover:underline font-medium">Política de Privacidad</a>
              </span>
            </label>

            <button type="submit" disabled={loading} className="btn-primary w-full py-3 bg-gradient-to-r from-medical-600 to-medical-500 hover:from-medical-500 hover:to-medical-400 shadow-lg shadow-medical-600/25">
              {loading ? "Enviando solicitud..." : "Solicitar acceso"}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-neutral-500">
            ¿Ya tienes cuenta?{" "}
            <Link href="/login" className="text-medical-600 hover:text-medical-700 font-semibold">Inicia sesión</Link>
          </p>
        </div>
      </div>
    </div>
  );
}