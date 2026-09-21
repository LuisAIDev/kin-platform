"use client";

import { Suspense, useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from "next/link";

function VerifyEmailPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get('token');
  const success = searchParams.get('success');
  const error = searchParams.get('error');
  const [resendEmail, setResendEmail] = useState('');
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle');
  const [resendError, setResendError] = useState('');
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    // Solo redirigir si hay token Y no hay success/error ya procesados
    if (token && !success && !error) {
      router.replace(`/verify-email/confirm?token=${token}`);
    }
  }, [token, success, error, router]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown(c => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  const handleResend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cooldown > 0 || !resendEmail.trim()) return;
    setResendState('sending');
    setResendError('');
    setCooldown(60);
    try {
      const res = await fetch('/api/v1/auth/resend-verification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: resendEmail.trim() }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || data.status === 'COOLDOWN' || data.status === 'NO_ACCOUNT') {
        setResendState('error');
        setResendError(data.status === 'COOLDOWN'
          ? 'Ya solicitaste un reenvío recientemente. Intenta en unos segundos.'
          : 'Si existe una cuenta asociada, recibirás un nuevo correo.');
        return;
      }
      setResendState('sent');
    } catch {
      setResendState('error');
      setResendError('No se pudo reenviar. Intenta de nuevo en unos minutos.');
    }
  };

  if (success === 'true') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center">
            <svg className="w-7 h-7 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900">Correo verificado correctamente</h1>
          <p className="text-sm text-neutral-600">Tu cuenta está activa. Ya puedes iniciar sesión.</p>
          <button
            onClick={() => router.push("/login")}
            className="rounded-lg bg-medical-600 py-2 px-6 text-sm font-medium text-white hover:bg-medical-700 transition min-h-11"
          >
            Ir a iniciar sesión
          </button>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
            <svg className="w-7 h-7 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900">No pudimos verificar tu correo</h1>
          <p className="text-sm text-neutral-600">{error}</p>
          <p className="text-sm text-neutral-600">Puedes solicitar un nuevo enlace de verificación.</p>
          <form onSubmit={handleResend} className="w-full mt-4">
            <input
              type="email"
              placeholder="Tu correo electrónico"
              value={resendEmail}
              onChange={(e) => setResendEmail(e.target.value)}
              required
              className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-medical-500 min-h-11 mb-2"
            />
            <button
              type="submit"
              disabled={cooldown > 0 || resendState === 'sending' || !resendEmail.trim()}
              className="w-full rounded-lg bg-medical-600 py-2 text-sm font-medium text-white hover:bg-medical-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {cooldown > 0 ? `Reenviar en ${cooldown}s` : 'Reenviar correo de verificación'}
            </button>
            {resendState === 'sent' && (
              <p className="mt-2 text-sm text-green-700">Te hemos enviado un nuevo correo. Revisa tu bandeja de entrada.</p>
            )}
            {resendState === 'error' && (
              <p className="mt-2 text-sm text-red-600">{resendError}</p>
            )}
          </form>
        </div>
      </div>
    );
  }

  if (token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-medical-100 flex items-center justify-center">
            <svg className="animate-spin w-6 h-6 text-medical-600" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
          </div>
          <p className="text-neutral-500">Redirigiendo a la verificación...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4">
      <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
        <div className="w-12 h-12 rounded-full bg-neutral-100 flex items-center justify-center">
          <svg className="w-7 h-7 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h1 className="text-2xl font-bold text-neutral-900">Enlace no reconocido</h1>
        <p className="text-sm text-neutral-600">No se encontró un token de verificación en el enlace.</p>
        <form onSubmit={handleResend} className="w-full mt-4">
          <input
            type="email"
            placeholder="Tu correo electrónico"
            value={resendEmail}
            onChange={(e) => setResendEmail(e.target.value)}
            required
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-medical-500 min-h-11 mb-2"
          />
          <button
            type="submit"
            disabled={cooldown > 0 || resendState === 'sending' || !resendEmail.trim()}
            className="w-full rounded-lg bg-medical-600 py-2 text-sm font-medium text-white hover:bg-medical-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {cooldown > 0 ? `Reenviar en ${cooldown}s` : 'Solicitar nuevo enlace de verificación'}
          </button>
          {resendState === 'sent' && (
            <p className="mt-2 text-sm text-green-700">Te hemos enviado un nuevo correo. Revisa tu bandeja de entrada.</p>
          )}
          {resendState === 'error' && (
            <p className="mt-2 text-sm text-red-600">{resendError}</p>
          )}
        </form>
        <p className="mt-4 text-center text-sm text-neutral-500">
          <Link href="/login" className="text-primary-600 underline hover:text-primary-700">
            Volver al inicio de sesión
          </Link>
        </p>
      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-neutral-50 px-4">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
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
      <VerifyEmailPageContent />
    </Suspense>
  );
}