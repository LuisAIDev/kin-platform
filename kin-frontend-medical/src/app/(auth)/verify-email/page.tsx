"use client";

import { Suspense, useEffect } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';

function VerifyEmailRedirectContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get('token');

  useEffect(() => {
    if (token) {
      router.replace(`/verify-email/confirm?token=${token}`);
    }
  }, [token, router]);

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

export default function VerifyEmailRedirect() {
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
      <VerifyEmailRedirectContent />
    </Suspense>
  );
}