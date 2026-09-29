import Link from 'next/link';
import { Shield, Clock, CheckCircle, FileText, ArrowLeft } from 'lucide-react';

interface PrivacyPolicyVersion {
  id: string;
  version: string;
  title: string;
  effective_date: string;
  active: boolean;
  published_at: string;
  published_by: string | null;
}

async function fetchVersions(): Promise<PrivacyPolicyVersion[]> {
  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'https://kin-backend-lwmy.onrender.com/api/v1';
  
  try {
    const response = await fetch(`${backendUrl}/public/privacy-policy/versions`, {
      cache: 'no-store',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new Error(`Error ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error fetching policy versions:', error);
    return [];
  }
}

function formatDate(dateString: string) {
  try {
    return new Date(dateString).toLocaleDateString('es-CO', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
}

export const metadata = {
  title: 'Versiones de Política de Privacidad | KIN Medical',
  description: 'Historial de versiones de la Política de Privacidad de KIN Medical.',
  openGraph: {
    title: 'Versiones de Política de Privacidad | KIN Medical',
    description: 'Historial de versiones de la Política de Privacidad de KIN Medical.',
    type: 'website',
  },
};

export default async function VersionsPage() {
  const versions = await fetchVersions();

  return (
    <div className="min-h-screen bg-white">
      {/* Navbar */}
      <nav className="sticky top-0 z-50 bg-white/80 backdrop-blur border-b border-neutral-200">
        <div className="max-w-4xl mx-auto flex items-center justify-between px-4 py-4">
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-medical-500 to-medical-600 flex items-center justify-center">
              <span className="text-white font-bold text-sm">K</span>
            </div>
            <span className="text-lg font-bold text-neutral-900">KIN Medical</span>
          </Link>
          <div className="hidden md:flex items-center gap-6">
            <Link href="/" className="text-neutral-700 hover:text-medical-600 transition">Inicio</Link>
            <Link href="/register/salud/paciente" className="text-neutral-700 hover:text-medical-600 transition">Para pacientes</Link>
            <Link href="/register/salud/medico" className="text-neutral-700 hover:text-medical-600 transition">Para médicos</Link>
            <Link href="/login" className="text-neutral-700 hover:text-medical-600 transition">Iniciar sesión</Link>
            <Link href="/register" className="inline-flex items-center justify-center rounded-lg bg-medical-600 px-4 py-2 text-sm font-semibold text-white hover:bg-medical-700 transition">
              Comenzar gratis
            </Link>
          </div>
        </div>
      </nav>

      <main className="max-w-4xl mx-auto px-4 py-12">
        {/* Header */}
        <header className="mb-8">
          <div className="flex items-center gap-4 mb-6">
            <Link 
              href="/privacy-policy" 
              className="inline-flex items-center gap-2 text-medical-600 hover:text-medical-700 transition"
            >
              <span className="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-neutral-100 text-medical-600 hover:bg-neutral-200 transition">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
              </span>
              <span className="font-medium text-medical-600 hover:text-medical-700">Política actual</span>
            </Link>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl md:text-4xl font-bold text-neutral-900">
                Historial de versiones
              </h1>
              <p className="text-neutral-600 mt-1">
                Historial completo de versiones de la Política de Privacidad de KIN Medical.
              </p>
            </div>
            <Link 
              href="/privacy-policy" 
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-medical-600 text-white font-semibold hover:bg-medical-700 transition"
            >
              <span>Ver política actual</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </div>
        </header>

        {/* Versions list */}
        <div className="bg-white border border-neutral-200 rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-neutral-50 border-b border-neutral-200">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-neutral-700 uppercase tracking-wider">Versión</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-neutral-700 uppercase tracking-wider">Título</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-neutral-700 uppercase tracking-wider">Fecha efectiva</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-neutral-700 uppercase tracking-wider">Publicada</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-neutral-700 uppercase tracking-wider">Estado</th>
                  <th className="px-6 py-4 text-right text-xs font-semibold text-neutral-700 uppercase tracking-wider w-24">Acción</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200">
                {versions.map((version) => (
                  <tr key={version.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="px-6 py-4">
                      <span className="font-mono font-semibold text-medical-600">{version.version}</span>
                    </td>
                    <td className="px-6 py-4 font-medium text-neutral-900">
                      {version.title}
                    </td>
                    <td className="px-6 py-4 text-neutral-700 whitespace-nowrap">
                      <span className="flex items-center gap-1.5">
                        <span className="w-4 h-4 text-neutral-400">📅</span>
                        <span>{new Date(version.effective_date).toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
                      </span>
                    </td>
                    <td className="px-6 py-4 text-neutral-700 whitespace-nowrap">
                      {new Date(version.published_at).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })}
                    </td>
                    <td className="px-6 py-4">
                      {version.active ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-green-50 text-green-700 text-xs font-medium">
                          <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
                          <span>Vigente</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-neutral-100 text-neutral-600 text-xs font-medium">
                          <span className="w-1.5 h-1.5 rounded-full bg-neutral-400" />
                          <span>Histórico</span>
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link
                        href={`/privacy-policy/${version.version}`}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-medical-600 hover:bg-medical-50 rounded-lg transition"
                      >
                        <span>Ver</span>
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </Link>
                    </td>
                  </tr>
                ))}
                {versions.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-6 py-12 text-center text-neutral-500">
                      <div className="flex flex-col items-center gap-3">
                        <svg className="w-12 h-12 text-neutral-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <p className="text-neutral-500">No hay versiones disponibles</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Footer */}
        <footer className="bg-neutral-900 text-neutral-400 py-12 mt-12">
          <div className="max-w-4xl mx-auto px-4 text-center">
            <Link href="/" className="flex items-center gap-2 mx-auto mb-4">
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-medical-500 to-medical-600 flex items-center justify-center">
                <span className="text-white font-bold text-sm">K</span>
              </div>
              <span className="text-lg font-bold text-white">KIN Medical</span>
            </Link>
            <p className="text-sm text-neutral-500">
              Plataforma de atención médica moderna para Latinoamérica.
            </p>
            <div className="mt-6 flex justify-center gap-6 text-sm">
              <Link href="/privacy-policy" className="hover:text-white transition">Política de Privacidad</Link>
              <Link href="/terms" className="hover:text-white transition">Términos de Uso</Link>
              <Link href="/contact" className="hover:text-white transition">Contacto</Link>
            </div>
            <p className="mt-6 text-xs text-neutral-600">
              © 2024 KIN Medical. Todos los derechos reservados.
            </p>
          </div>
        </footer>
      </main>
    </div>
  );
}
