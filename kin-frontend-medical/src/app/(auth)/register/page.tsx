"use client";

import Link from "next/link";

export default function RegisterPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4 py-12">
      <div className="w-full max-w-lg bg-white rounded-2xl shadow-xl border border-neutral-200 p-8 sm:p-10">
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-2 mb-6">
            <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-medical-500 to-medical-600 flex items-center justify-center">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <path d="M9 12l2 2 4-4" />
              </svg>
            </div>
            <span className="text-xl font-bold tracking-tight text-neutral-900">KIN Medical</span>
          </Link>
          <h1 className="text-2xl font-bold text-neutral-900">Crear cuenta</h1>
          <p className="text-neutral-500 mt-2">¿Cómo quieres usar KIN Medical?</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Link
            href="/auth/register/salud/paciente"
            className="p-6 rounded-xl border-2 border-neutral-200 hover:border-medical-500 hover:bg-medical-50 transition-all text-center"
          >
            <div className="w-12 h-12 rounded-xl bg-medical-100 flex items-center justify-center mx-auto mb-3">
              <svg className="w-7 h-7 text-medical-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-neutral-900">Paciente</h3>
            <p className="text-sm text-neutral-500 mt-1">Gestiona tu salud</p>
          </Link>

          <Link
            href="/auth/register/salud/medico"
            className="p-6 rounded-xl border-2 border-neutral-200 hover:border-medical-500 hover:bg-medical-50 transition-all text-center"
          >
            <div className="w-12 h-12 rounded-xl bg-medical-100 flex items-center justify-center mx-auto mb-3">
              <svg className="w-7 h-7 text-medical-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-neutral-900">Médico</h3>
            <p className="text-sm text-neutral-500 mt-1">Gestiona tu práctica</p>
          </Link>
        </div>

        <p className="mt-8 text-center text-sm text-neutral-500">
          ¿Ya tienes cuenta?{" "}
          <Link href="/login" className="text-primary-600 hover:text-primary-700 font-medium">
            Inicia sesión
          </Link>
        </p>
      </div>
    </div>
  );
}