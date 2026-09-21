"use client";

import { Suspense, useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from "next/link";

function CheckEmailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const email = searchParams.get('email');
  const pending = searchParams.get('pending') === '1';
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown(c => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  const handleResend = async () => {
    if (cooldown > 0 || !email?.trim()) return;
    setCooldown(60);
    try {
      const res = await fetch('/api/v1/auth/resend-verification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email!.trim() }),
      });
      if (!res.ok) throw new Error('No se pudo reenviar');
    } catch {
      // Silencioso: el cooldown ya empezó
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4 py-12">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-neutral-200 p-8 text-center">
        <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <h1 className="text-2xl font-bold text-neutral-900 mb-2">
          {pending ? 'Registro enviado a revisión' : '¡Cuenta creada!'}
        </h1>
        <p className="text-neutral-600 mb-6">
          {pending
            ? 'Tu solicitud de registro como médico está <strong>pendiente de revisión</strong>. Un administrador verificará tu cédula profesional y te avisaremos por correo.'
            : `Hemos enviado un correo de verificación a <strong className="text-neutral-900">${email || 'tu correo'}</strong>. Revisa tu bandeja de entrada (y carpeta de spam) y haz clic en el enlace para activar tu cuenta.`}
        </p>

        {!pending && (
          <div className="mb-4 p-3 rounded-lg bg-neutral-50 border border-neutral-200 text-sm text-neutral-600">
            <p>¿No llegó el correo?</p>
            <button
              onClick={handleResend}
              disabled={cooldown > 0}
              className="mt-2 text-medical-600 underline hover:text-medical-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {cooldown > 0
                ? `Reenviar en ${cooldown}s`
                : 'Reenviar correo de verificación'}
            </button>
          </div>
        )}

        <Link
          href="/login"
          className="inline-flex items-center justify-center rounded-lg bg-medical-600 py-2 px-6 text-sm font-medium text-white hover:bg-medical-700 transition min-h-11 w-full"
        >
          Ir a iniciar sesión
        </Link>

        <p className="mt-4 text-center text-sm text-neutral-500">
          ¿Ya verificaste tu correo?{" "}
          <Link href="/verify-email" className="text-primary-600 underline hover:text-primary-700">
            Verifica tu cuenta aquí
          </Link>
        </p>
      </div>
    </div>
  );
}

export default function CheckEmailPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4">
        <div className="w-full max-w-md flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-medical-100 flex items-center justify-center">
            <svg className="animate-spin w-6 h-6 text-medical-600" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
          </div>
          <p className="text-neutral-500">Cargando...</p>
        </div>
      </div>
    }>
      <CheckEmailContent />
    </Suspense>
  );
}