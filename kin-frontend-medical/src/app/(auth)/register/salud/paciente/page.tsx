"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";
import { PasswordInput } from "@/components/ui/PasswordInput";

export default function PatientRegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    dateOfBirth: "",
    sex: "",
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
    const { error } = await authService.registerPatient({
      fullName: form.fullName,
      email: form.email,
      password: form.password,
      dateOfBirth: form.dateOfBirth,
      sex: form.sex,
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
                <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-neutral-900">Registro de Paciente</h1>
            <p className="text-neutral-500 mt-2">Completa tus datos para crear tu cuenta</p>
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
                <label htmlFor="dateOfBirth" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Fecha de nacimiento
                </label>
                <input id="dateOfBirth" name="dateOfBirth" type="date" required value={form.dateOfBirth} onChange={handleChange} className="input-field" max={new Date().toISOString().split("T")[0]} />
              </div>
              <div>
                <label htmlFor="sex" className="block text-sm font-medium text-neutral-700 mb-1.5">
                  Sexo
                </label>
                <select id="sex" name="sex" required value={form.sex} onChange={handleChange} className="input-field">
                  <option value="">Selecciona</option>
                  <option value="M">Masculino</option>
                  <option value="F">Femenino</option>
                  <option value="O">Otro</option>
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-neutral-700 mb-1.5">
                Teléfono (opcional)
              </label>
              <input id="phone" name="phone" type="tel" value={form.phone} onChange={handleChange} className="input-field" placeholder="+34 600 000 000" />
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
              {loading ? "Creando cuenta..." : "Crear cuenta"}
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