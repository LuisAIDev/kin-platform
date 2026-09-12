'use client';

import { useEffect, useState } from 'react';
import { Clock, CheckCircle2, XCircle } from 'lucide-react';
import Link from 'next/link';

export default function VerificationStatusPage() {
  const [status, setStatus] = useState<'PENDING' | 'APPROVED' | 'REJECTED' | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const token = localStorage.getItem('kin_user_v2');
const res = await fetch("/api/v1/health/physician/application", { credentials: "include" });
        if (res.ok) {
          const data = await res.json();
          const verificationStatus = data.physicianVerificationStatus || data.status;
          if (verificationStatus === 'PENDING') {
            setStatus('PENDING');
          } else if (verificationStatus === 'APPROVED') {
            setStatus('APPROVED');
          } else if (verificationStatus === 'REJECTED') {
            setStatus('REJECTED');
          } else {
            setStatus(null);
          }
        }
      } catch {
        setStatus(null);
      } finally {
        setLoading(false);
      }
    };
    fetchStatus();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-medical-50 via-white to-accent-50">
      <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-neutral-900 mb-6 text-center">Estado de Verificación</h1>

        {loading && (
          <div className="bg-white rounded-2xl shadow-xl border border-neutral-200 p-8 text-center">
            <div className="animate-spin h-12 w-12 border-4 border-medical-500 border-t-transparent rounded-full mx-auto mb-4" />
            <p className="text-neutral-500">Consultando estado de verificación...</p>
          </div>
        )}

        {!loading && status === 'PENDING' && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-2xl p-8">
            <Clock className="w-12 h-12 text-yellow-600 mb-4" />
            <h2 className="text-xl font-semibold text-yellow-900 mb-2">Verificación Pendiente</h2>
            <p className="text-yellow-700">
              Tu cédula está siendo verificada por nuestro equipo. Este proceso puede tardar hasta 24 horas.
              Te notificaremos por correo cuando esté completa.
            </p>
            <div className="mt-6">
              <Link href="/dashboard/physician" className="btn-primary inline-block px-6 py-3">
                Volver al Dashboard
              </Link>
            </div>
          </div>
        )}

        {!loading && status === 'APPROVED' && (
          <div className="bg-green-50 border border-green-200 rounded-2xl p-8">
            <CheckCircle2 className="w-12 h-12 text-green-600 mb-4" />
            <h2 className="text-xl font-semibold text-green-900 mb-2">Verificación Aprobada</h2>
            <p className="text-green-700 mb-6">
              ¡Bienvenido a KIN Medical! Ya puedes acceder a tu Portal Médico.
            </p>
            <div className="mt-6">
              <Link href="/dashboard/physician" className="btn-primary inline-block px-6 py-3">
                Ir al Dashboard
              </Link>
            </div>
          </div>
        )}

        {!loading && status === 'REJECTED' && (
          <div className="bg-red-50 border border-red-200 rounded-2xl p-8">
            <XCircle className="w-12 h-12 text-red-600 mb-4" />
            <h2 className="text-xl font-semibold text-red-900 mb-2">Verificación Rechazada</h2>
            <p className="text-red-700 mb-6">
              Tu cédula no pudo ser verificada. Por favor, contacta a soporte para más información.
            </p>
            <div className="mt-6 flex flex-col sm:flex-row gap-4 justify-center">
              <Link href="/dashboard/physician" className="btn-primary inline-block px-6 py-3">
                Volver al Dashboard
              </Link>
              <Link href="#" className="btn-secondary inline-block px-6 py-3">
                Contactar Soporte
              </Link>
            </div>
          </div>
        )}

        {!loading && status === null && (
          <div className="bg-white rounded-2xl shadow-xl border border-neutral-200 p-8 text-center">
            <p className="text-neutral-500 mb-4">No se encontró una solicitud de verificación.</p>
            <Link href="/register/salud/medico" className="btn-primary inline-block px-6 py-3">
              Solicitar Capacidad Profesional
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
