"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";
import { PasswordInput } from "@/components/ui/PasswordInput";
import { ValidatedInput } from "@/components/ui/ValidatedInput";
import { RegisterWizard } from "@/components/auth/RegisterWizard";

const STEPS = [
  { id: 1, title: "Personales", description: "Nombre, email y contraseña" },
  { id: 2, title: "Profesional", description: "Cédula, especialidad y país" },
  { id: 3, title: "Verificación", description: "Cédula y consentimiento" },
];

const COUNTRY_LICENSE_PATTERNS: Record<string, { pattern: RegExp; label: string }> = {
  CO: { pattern: /^\d{6,12}$/, label: "Colombia (6-12 dígitos)" },
  MX: { pattern: /^\d{7,10}$/, label: "México (7-10 dígitos)" },
  ES: { pattern: /^\d{8}[A-Z]$/, label: "España (8 dígitos + letra)" },
};

export default function PhysicianRegisterPage() {
  const router = useRouter();
  const [currentStep, setCurrentStep] = useState(1);
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
  const [licenseValidation, setLicenseValidation] = useState<{ valid: boolean; message?: string } | null>(null);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const target = e.target as HTMLInputElement;
    const value = target.value;
    setForm({ ...form, [target.name]: target.type === "checkbox" ? target.checked : value });

    if (target.name === "licenseNumber" || target.name === "country") {
      const currentCountry = target.name === "country" ? value : form.country;
      const spec = COUNTRY_LICENSE_PATTERNS[currentCountry];
      if (spec && (target.name === "country" ? form.licenseNumber : value)) {
        const licenseValue = target.name === "country" ? form.licenseNumber : value;
        if (!spec.pattern.test(licenseValue)) {
          setLicenseValidation({ valid: false, message: `La cédula profesional no es válida${currentCountry ? ` para ${spec.label}` : ''}.` });
        } else {
          setLicenseValidation({ valid: true, message: "Cédula válida ✓" });
        }
      }
    }
    if (target.name === "country") {
      const spec = COUNTRY_LICENSE_PATTERNS[value];
      if (spec && form.licenseNumber) {
        if (!spec.pattern.test(form.licenseNumber)) {
          setLicenseValidation({ valid: false, message: `La cédula profesional no es válida para ${spec.label}.` });
        } else {
          setLicenseValidation({ valid: true, message: "Cédula válida ✓" });
        }
      }
    }
  };

  const validateStep1 = () => {
    if (!form.fullName.trim()) return "El nombre completo es obligatorio.";
    if (!form.email.includes("@") || !form.email.includes(".")) return "El correo electrónico no es válido. Verifica que tenga el formato correcto (ej. nombre@dominio.com).";
    if (!form.password || form.password.length < 8) return "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número.";
    return null;
  };

  const validateStep2 = () => {
    if (!form.licenseNumber.trim()) return "El número de cédula profesional es obligatorio.";
    if (!form.specialty.trim()) return "Debes seleccionar una especialidad. Si eres médico general, selecciona 'Medicina General'.";
    if (!form.country) return "Debes seleccionar un país.";
    return null;
  };

  const handleNext = () => {
    if (currentStep === 1) {
      const err = validateStep1();
      if (err) { setError(err); return; }
    }
    if (currentStep === 2) {
      const err = validateStep2();
      if (err) { setError(err); return; }
    }
    setError(null);
    if (currentStep < 3) setCurrentStep(currentStep + 1);
  };

  const handleBack = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1);
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
    if (error) { setError(error); return; }
    router.push("/auth/verify-email");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-medical-50 via-white to-accent-50">
      <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="text-center mb-8">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-medical-500 to-medical-600">
            <svg className="h-7 w-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900">Registro de Médico</h1>
          <p className="text-neutral-500 mt-2">Solicita tu acceso profesional a KIN Medical</p>
        </div>

        <RegisterWizard steps={STEPS} currentStep={currentStep}>
          {error && (
            <div className="mb-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm" role="alert">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {currentStep === 1 && (
              <div className="space-y-4 animate-in fade-in duration-300">
                <ValidatedInput
                  id="fullName"
                  name="fullName"
                  label="Nombre completo"
                  value={form.fullName}
                  onChange={handleChange}
                  placeholder="Juan Pérez"
                  required
                />
                <ValidatedInput
                  id="email"
                  name="email"
                  type="email"
                  label="Correo electrónico"
                  value={form.email}
                  onChange={handleChange}
                  placeholder="tu@email.com"
                  autoComplete="email"
                  required
                  validation={(v) => {
                    if (!v.includes("@") || !v.includes(".")) return { valid: false, message: "El correo electrónico no es válido. Verifica que tenga el formato correcto (ej. nombre@dominio.com)." };
                    return { valid: true, message: "Correo válido ✓" };
                  }}
                />
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
                  validation={(v) => {
                    const hasUpper = /[A-Z]/.test(v);
                    const hasNumber = /\d/.test(v);
                    if (v.length > 0 && (v.length < 8 || !hasUpper || !hasNumber)) {
                      return { valid: false, message: "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número." };
                    }
                    return { valid: true, message: "Contraseña válida ✓" };
                  }}
                />
                <div className="flex justify-end pt-2">
                  <button type="button" onClick={handleNext} className="btn-primary px-6 py-3">Siguiente →</button>
                </div>
              </div>
            )}

            {currentStep === 2 && (
              <div className="space-y-4 animate-in fade-in duration-300">
                <ValidatedInput
                  id="licenseNumber"
                  name="licenseNumber"
                  label="Nº de cédula profesional"
                  value={form.licenseNumber}
                  onChange={handleChange}
                  placeholder="Ej: 12345678"
                  required
                  validation={(v) => {
                    if (v.length < 6) return { valid: false, message: "La cédula profesional no es válida. Verifica que tenga entre 6 y 12 dígitos." };
                    return { valid: true, message: "Cédula válida ✓" };
                  }}
                />
                <ValidatedInput
                  id="specialty"
                  name="specialty"
                  label="Especialidad"
                  value={form.specialty}
                  onChange={handleChange}
                  placeholder="Medicina General"
                  required
                  validation={(v) => {
                    if (!v.trim()) return { valid: false, message: "Debes seleccionar una especialidad. Si eres médico general, selecciona 'Medicina General'." };
                    return { valid: true, message: "" };
                  }}
                />
                <div>
                  <label htmlFor="country" className="block text-sm font-medium text-neutral-700 mb-1.5">País</label>
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
                  <label htmlFor="phone" className="block text-sm font-medium text-neutral-700 mb-1.5">Teléfono (opcional)</label>
                  <input id="phone" name="phone" type="tel" value={form.phone} onChange={handleChange} className="input-field" placeholder="+34 600 000 000" />
                </div>
                {licenseValidation && (
                  <div className={`text-sm ${licenseValidation.valid ? 'text-green-600' : 'text-red-600'}`}>
                    {licenseValidation.message}
                  </div>
                )}
                <div className="flex justify-between pt-2">
                  <button type="button" onClick={handleBack} className="btn-secondary px-6 py-3">← Atrás</button>
                  <button type="button" onClick={handleNext} className="btn-primary px-6 py-3">Siguiente →</button>
                </div>
              </div>
            )}

            {currentStep === 3 && (
              <div className="space-y-6 animate-in fade-in duration-300">
                <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 mb-4">
                  <p className="text-sm text-yellow-800">
                    <strong>Tu cédula está siendo verificada.</strong> Este proceso puede tardar hasta 24 horas. Te notificaremos por correo cuando esté completa.
                  </p>
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
                <div className="flex justify-between pt-2">
                  <button type="button" onClick={handleBack} className="btn-secondary px-6 py-3">← Atrás</button>
                  <button type="submit" disabled={loading} className="btn-primary px-6 py-3 bg-gradient-to-r from-medical-600 to-medical-500 hover:from-medical-500 hover:to-medical-400 shadow-lg shadow-medical-600/25">
                    {loading ? "Enviando solicitud..." : "Solicitar acceso ✓"}
                  </button>
                </div>
              </div>
            )}
          </form>
        </RegisterWizard>

        <p className="mt-6 text-center text-sm text-neutral-500">
          ¿Ya tienes cuenta?{" "}
          <Link href="/login" className="text-medical-600 hover:text-medical-700 font-semibold">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}
