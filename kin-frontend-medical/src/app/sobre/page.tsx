import Link from 'next/link';
import { Shield, Heart, Brain, Users, Stethoscope, Building2, Lock, Database, Zap, TrendingUp } from 'lucide-react';

export const metadata = {
  title: 'Sobre KIN Medical',
  description: 'Conoce la historia, filosofía y capacidades de KIN Medical — la plataforma de atención médica moderna para Latinoamérica.',
};

export default function SobrePage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Navbar */}
      <nav className="sticky top-0 z-50 bg-white/80 backdrop-blur border-b border-neutral-200">
        <div className="max-w-7xl mx-auto flex items-center justify-between px-4 py-4">
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

      {/* Hero */}
      <section className="bg-gradient-to-b from-medical-50 to-white py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h1 className="text-4xl md:text-5xl font-bold text-neutral-900 mb-4">
            Sobre KIN Medical
          </h1>
          <p className="text-xl text-neutral-600 max-w-2xl mx-auto">
            La plataforma que ayuda a médicos, pacientes y clínicas a gestionar la atención médica moderna en Latinoamérica.
          </p>
        </div>
      </section>

      {/* ¿Qué es? */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">¿Qué es KIN Medical?</h2>
          <div className="prose prose-lg max-w-none text-neutral-700 space-y-4">
            <p>
              KIN Medical es un <strong>sistema operativo de atención médica moderna</strong> que unifica en una sola plataforma:
            </p>
            <ul className="list-disc pl-6 space-y-2">
              <li>Triaje digital con motor determinista</li>
              <li>Portal médico con gestión de pacientes</li>
              <li>Portal del paciente con historial clínico</li>
              <li>Telemedicina con mensajería segura</li>
              <li>Agenda, citas y seguimiento</li>
              <li>Análisis de documentos clínicos con OCR</li>
            </ul>
            <p className="mt-6">
              KIN Medical no es un chatbot médico. Es una <strong>plataforma tecnológica</strong> donde la IA es un componente dentro de una arquitectura mayor, con un motor determinista que decide y una capa de IA que comunica.
            </p>
            <div className="bg-medical-50 border-l-4 border-medical-600 p-6 my-8">
              <p className="text-lg font-semibold text-medical-900 italic">
                «Java decide. El LLM únicamente comunica.»
              </p>
              <p className="text-sm text-medical-700 mt-2">
                Principio que guía toda la arquitectura de KIN Medical.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Para quién */}
      <section className="py-16 px-4 bg-neutral-50">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Para quién es</h2>
          <div className="grid md:grid-cols-2 gap-6">
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center mb-4">
                <Stethoscope className="w-6 h-6 text-medical-600" />
              </div>
              <h3 className="font-semibold text-lg text-neutral-900 mb-2">Médicos</h3>
              <p className="text-neutral-600 text-sm">
                Digitaliza tu consulta, gestiona pacientes, automatiza seguimiento y ofrece telemedicina.
              </p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center mb-4">
                <Users className="w-6 h-6 text-medical-600" />
              </div>
              <h3 className="font-semibold text-lg text-neutral-900 mb-2">Pacientes</h3>
              <p className="text-neutral-600 text-sm">
                Accede a tu historial, agenda citas, recibe triaje digital y comparte documentos.
              </p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center mb-4">
                <Building2 className="w-6 h-6 text-medical-600" />
              </div>
              <h3 className="font-semibold text-lg text-neutral-900 mb-2">Clínicas</h3>
              <p className="text-neutral-600 text-sm">
                Multi-médico, multi-sede, dashboards institucionales (próximamente).
              </p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center mb-4">
                <TrendingUp className="w-6 h-6 text-medical-600" />
              </div>
              <h3 className="font-semibold text-lg text-neutral-900 mb-2">IPS / EPS</h3>
              <p className="text-neutral-600 text-sm">
                Reportes regulatorios, indicadores poblacionales e integración con redes de atención (futuro).
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Filosofía */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Nuestra filosofía</h2>
          <div className="space-y-6">
            <div className="flex gap-4">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center flex-shrink-0">
                <Brain className="w-6 h-6 text-medical-600" />
              </div>
              <div>
                <h3 className="font-semibold text-lg text-neutral-900 mb-2">Determinismo antes que IA</h3>
                <p className="text-neutral-600">
                  Las decisiones clínicas del triaje las toma un motor de reglas fijas basado en guías médicas verificables (CIE-10, OMS, protocolos clínicos). La IA solo traduce el resultado a lenguaje sencillo.
                </p>
              </div>
            </div>
            <div className="flex gap-4">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center flex-shrink-0">
                <Shield className="w-6 h-6 text-medical-600" />
              </div>
              <div>
                <h3 className="font-semibold text-lg text-neutral-900 mb-2">Trazabilidad y auditabilidad</h3>
                <p className="text-neutral-600">
                  Cada decisión clínica queda registrada y es auditable. Cumplimos con Habeas Data y estándares de protección de datos de salud.
                </p>
              </div>
            </div>
            <div className="flex gap-4">
              <div className="w-12 h-12 rounded-lg bg-medical-100 flex items-center justify-center flex-shrink-0">
                <Heart className="w-6 h-6 text-medical-600" />
              </div>
              <div>
                <h3 className="font-semibold text-lg text-neutral-900 mb-2">Humanidad primero</h3>
                <p className="text-neutral-600">
                  KIN no reemplaza al médico. Lo potencia con herramientas que respetan su criterio y refuerzan la relación con sus pacientes.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stack técnico */}
      <section className="py-16 px-4 bg-neutral-50">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Capacidades técnicas</h2>
          <div className="grid md:grid-cols-2 gap-4">
            {[
              { icon: Zap, title: 'Triaje con motor determinista', desc: 'Pipeline clínico con reglas verificables' },
              { icon: Database, title: 'Historial clínico digital', desc: 'PostgreSQL + migraciones Flyway' },
              { icon: Lock, title: 'Seguridad JWT + cookies HttpOnly', desc: 'Spring Security + auditoría' },
              { icon: Brain, title: 'IA para comunicación', desc: 'DeepSeek vía Spring AI (solo redacta)' },
            ].map((item, idx) => (
              <div key={idx} className="bg-white rounded-xl p-4 border border-neutral-200 flex gap-3">
                <item.icon className="w-5 h-5 text-medical-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-semibold text-neutral-900">{item.title}</h4>
                  <p className="text-sm text-neutral-600">{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Creado por */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Creado por</h2>
          <div className="bg-white rounded-2xl border border-neutral-200 p-8 max-w-2xl mx-auto">
            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-medical-500 to-medical-600 flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl font-bold text-white">LG</span>
            </div>
            <h3 className="text-xl font-bold text-neutral-900">Luis Orlando Guerra González</h3>
            <p className="text-neutral-500 mt-1">Desarrollador de software</p>
            <p className="text-neutral-600 mt-4 text-sm">
              Tecnólogo en Análisis y Desarrollo de Software. KIN Medical nació como una iniciativa 
              para mejorar la comunicación entre salud y tecnología en Latinoamérica.
            </p>
          </div>
        </div>
      </section>

      {/* CTA final */}
      <section className="py-16 px-4 bg-medical-600 text-white">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl font-bold mb-4">Comienza hoy con KIN Medical</h2>
          <p className="text-medical-50 mb-8 max-w-2xl mx-auto">
            Únete a los médicos y pacientes que ya usan KIN Medical.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/register/salud/paciente"
              className="inline-flex items-center justify-center rounded-lg bg-white px-6 py-3 font-semibold text-medical-700 hover:bg-medical-50 transition"
            >
              Soy paciente
            </Link>
            <Link
              href="/register/salud/medico"
              className="inline-flex items-center justify-center rounded-lg border-2 border-white px-6 py-3 font-semibold text-white hover:bg-white hover:text-medical-700 transition"
            >
              Soy médico
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
