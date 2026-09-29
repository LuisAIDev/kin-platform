import Link from 'next/link';
import { Shield, FileText, Clock, ArrowLeft, Download } from 'lucide-react';
import { PrivacyPolicyViewer } from '@/components/privacy/PrivacyPolicyViewer';

interface PrivacyPolicyData {
  id: string;
  version: string;
  title: string;
  content_md: string;
  effective_date: string;
  active: boolean;
  published_at: string;
  published_by: string | null;
}

async function fetchActivePolicy(): Promise<PrivacyPolicyData | null> {
  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'https://kin-backend-lwmy.onrender.com/api/v1';
  
  try {
    const response = await fetch(`${backendUrl}/public/privacy-policy`, {
      cache: 'no-store',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!response.ok) {
      if (response.status === 404) return null;
      throw new Error(`Error ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error fetching privacy policy:', error);
    return null;
  }
}

export const metadata = {
  title: 'Política de Privacidad | KIN Medical',
  description: 'Política de privacidad y protección de datos personales de KIN Medical. Conoce tus derechos bajo la Ley 1581 de 2012.',
  openGraph: {
    title: 'Política de Privacidad | KIN Medical',
    description: 'Política de privacidad y protección de datos personales de KIN Medical.',
    type: 'website',
  },
};

function PolicyContent({ policy }: { policy: PrivacyPolicyData }) {
  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString('es-CO', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return dateString;
    }
  };

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

      {/* Header */}
      <header className="bg-neutral-50 border-b border-neutral-200">
        <div className="max-w-4xl mx-auto px-4 py-12">
          <div className="flex items-center gap-4 mb-6">
            <Link 
              href="/privacy-policy/versions" 
              className="inline-flex items-center gap-2 text-medical-600 hover:text-medical-700 transition"
            >
              <ArrowLeft className="w-5 h-5" />
              <span className="font-medium">Volver a versiones</span>
            </Link>
          </div>
          
          <div className="text-center">
            <div className="inline-flex items-center gap-2 text-sm text-medical-600 mb-4">
              <Shield className="w-5 h-5" />
              <span className="font-semibold">Política de Privacidad</span>
            </div>
          </div>
          
          <h1 className="text-3xl md:text-4xl font-bold text-neutral-900 mb-2">
            {policy.title}
          </h1>
          
          <div className="flex items-center justify-center gap-6 text-sm text-neutral-500 mt-4">
            <div className="flex items-center gap-1.5">
              <span className="font-medium">Versión:</span>
              <span className="font-mono text-medical-600">{policy.version}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Clock className="w-4 h-4" />
              <span className="font-medium">Vigente desde:</span>
              <span className="font-mono">{new Date(policy.effective_date).toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
            </div>
            <div className="inline-flex items-center gap-1.5 px-2 py-1 rounded-full bg-green-50 text-green-700">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
              <span className="text-xs font-medium">Vigente</span>
            </div>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-4 py-12">
        <div className="bg-white border border-neutral-200 rounded-xl p-8 md:p-12">
          <PrivacyPolicyViewer content={policy.content_md} />
        </div>

        {/* Footer info */}
        <div className="mt-8 p-6 bg-neutral-50 rounded-xl border border-neutral-200">
          <h3 className="font-semibold text-neutral-900 mb-3">Tus derechos (Ley 1581 de 2012)</h3>
          <div className="grid sm:grid-cols-2 gap-4 text-sm text-neutral-700">
            <div className="flex items-start gap-2">
              <span className="font-medium text-medical-600">1.</span>
              <span>Derecho de acceso: conocer tus datos personales.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="font-medium text-medical-600">2.</span>
              <span>Derecho de rectificación: corregir datos inexactos.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="font-medium text-medical-600">3.</span>
              <span>Derecho de supresión: eliminar tus datos (con excepciones legales).</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="font-medium text-medical-600">4.</span>
              <span>Derecho a revocar el consentimiento.</span>
            </div>
          </div>
        </div>

        <div className="mt-6 p-6 bg-medical-50 border border-medical-200 rounded-xl">
          <h3 className="font-semibold text-medical-900 mb-2 flex items-center gap-2">
            <span>📧</span>
            Contacto
          </h3>
          <p className="text-medical-700">
            Para ejercer tus derechos o consultas sobre esta política, contacta a nuestro Delegado de Protección de Datos:
          </p>
          <p className="mt-2 font-mono text-medical-700">privacidad@kin-platform.com</p>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-neutral-900 text-neutral-400 py-12 mt-auto">
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
    </div>
  );
}

export default async function PrivacyPolicyPage() {
  const policy = await fetchActivePolicy();
  
  if (!policy) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="max-w-md mx-auto px-4 text-center">
          <div className="w-16 h-16 rounded-full bg-medical-100 flex items-center justify-center mx-auto mb-4">
            <span className="text-2xl">📄</span>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mb-2">Política no disponible</h1>
          <p className="text-neutral-600 mb-6">
            No se pudo cargar la política de privacidad. Por favor, intente nuevamente más tarde.
          </p>
          <a href="/" className="inline-flex items-center justify-center rounded-lg bg-medical-600 px-6 py-3 font-semibold text-white hover:bg-medical-700 transition">
            Volver al inicio
          </a>
        </div>
      </div>
    );
  }
}
