"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";

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
    <div className="min-h-screen bg-neutral-50">
      <div className="max-w-2xl mx-auto px-4 py-12">
        <div className="bg-white rounded-2xl shadow-sm border border-neutral-200 p-8">
          <div className="text-center mb-8">
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

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-neutral-700 mb-1.5">
                Contraseña
              </label>
              <input id="password" name="password" type="password" autoComplete="new-password" required minLength={8} value={form.password} onChange={handleChange} className="input-field" />
            </div>

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
                <a href="/privacidad" className="text-primary-600 hover:underline">Política de Privacidad</a>
              </span>
            </label>

            <button type="submit" disabled={loading} className="btn-primary w-full py-3">
              {loading ? "Enviando solicitud..." : "Solicitar acceso"}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-neutral-500">
            ¿Ya tienes cuenta?{" "}
            <Link href="/login" className="text-primary-600 hover:text-primary-700 font-medium">Inicia sesión</Link>
          </p>
        </div>
      </div>
    </div>
  );
}