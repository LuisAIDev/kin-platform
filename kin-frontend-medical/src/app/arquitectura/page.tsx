import Link from 'next/link';
import { 
  Stethoscope, Brain, Shield, Database, Zap, Lock, 
  FileText, MessageCircle, Users, Activity, 
  CheckCircle2, Server, Cloud, Code2, TestTube 
} from 'lucide-react';

export const metadata = {
  title: 'Arquitectura Técnica — KIN Medical',
  description: 'Arquitectura determinista, IA aplicada, OCR médico, seguridad, testing automatizado y cloud deployment en KIN Medical.',
};

export default function ArquitecturaPage() {
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
            Arquitectura Técnica de KIN Medical
          </h1>
          <p className="text-xl text-neutral-600 max-w-2xl mx-auto">
            Plataforma médica construida con motor determinista, IA aplicada con guardrails, OCR clínico, seguridad por diseño y cloud deployment.
          </p>
          <div className="flex flex-wrap gap-2 justify-center mt-8">
            {[
              'Java / Spring Boot',
              'Next.js / React / TypeScript',
              'PostgreSQL',
              'Google Vision OCR',
              'DeepSeek IA',
              'Testing automatizado',
              'CI/CD',
              'Security by Design',
              'Cloud deployment',
            ].map((tag) => (
              <span key={tag} className="px-3 py-1 rounded-full bg-medical-100 text-medical-700 text-sm font-medium">
                {tag}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* Resumen ejecutivo */}
      <section className="py-16 px-4 bg-neutral-50">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-8 text-center">
            KIN Medical en una mirada técnica
          </h2>
          <div className="grid md:grid-cols-2 gap-6">
            {[
              { n: '1', title: 'El problema', desc: 'Fragmentación de información clínica entre historia médica, agenda, laboratorios y comunicación médico-paciente.' },
              { n: '2', title: 'La arquitectura', desc: 'Backend Spring Boot, frontend Next.js, motor determinista para triaje y pipeline clínico de etapas.' },
              { n: '3', title: 'La IA', desc: 'El LLM redacta el análisis ya decidido. El motor Java decide la clasificación y urgencia del triaje.' },
              { n: '4', title: 'El control', desc: 'Verificación manual de médicos, protección de datos clínicos, testing automatizado y DevSecOps.' },
            ].map((item) => (
              <div key={item.n} className="bg-white rounded-xl p-6 border border-neutral-200">
                <div className="w-10 h-10 rounded-full bg-medical-600 text-white flex items-center justify-center font-bold mb-4">
                  {item.n}
                </div>
                <h3 className="font-semibold text-lg text-neutral-900 mb-2">{item.title}</h3>
                <p className="text-neutral-600 text-sm">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Principio central */}
      <section className="py-16 px-4 bg-medical-600 text-white">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl md:text-4xl font-bold mb-6">
            Java decide. El LLM únicamente comunica.
          </h2>
          <p className="text-lg text-medical-50 max-w-2xl mx-auto">
            KIN Medical no delega las decisiones clínicas al modelo de IA. Un motor determinista de reglas fijas (basado en CIE-10, OMS y protocolos clínicos) decide la clasificación, urgencia y recomendaciones del triaje. La IA solo redacta el resultado en lenguaje sencillo.
          </p>
        </div>
      </section>

      {/* Flujo del triaje */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-8 text-center">
            El flujo del triaje digital
          </h2>
          <div className="space-y-4">
            {[
              { n: 1, title: 'Paciente', desc: 'Ingresa síntomas en el formulario de triaje digital.' },
              { n: 2, title: 'Motor determinista', desc: 'Aplica reglas fijas basadas en CIE-10, OMS y protocolos clínicos.' },
              { n: 3, title: 'Clasificación', desc: 'Calcula condiciones probables, severidad y urgencia.' },
              { n: 4, title: 'Pruebas sugeridas', desc: 'Sugiere exámenes complementarios según la condición.' },
              { n: 5, title: 'Fuentes médicas', desc: 'Proporciona enlaces a MedlinePlus, Mayo Clinic, MSD Manuals, CDC, OMS, AAFP.' },
              { n: 6, title: 'IA', desc: 'El LLM (DeepSeek) redacta el resultado en lenguaje sencillo.' },
              { n: 7, title: 'Resultado', desc: 'El paciente ve el análisis completo con disclaimer legal.' },
            ].map((step) => (
              <div key={step.n} className="flex gap-4 items-start">
                <div className="w-10 h-10 rounded-full bg-medical-100 text-medical-700 flex items-center justify-center font-bold flex-shrink-0">
                  {step.n}
                </div>
                <div>
                  <h3 className="font-semibold text-neutral-900">{step.title}</h3>
                  <p className="text-neutral-600 text-sm">{step.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* OCR médico */}
      <section className="py-16 px-4 bg-neutral-50">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-8 text-center">
            Análisis de documentos clínicos con OCR
          </h2>
          <div className="grid md:grid-cols-3 gap-6">
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <FileText className="w-8 h-8 text-medical-600 mb-4" />
              <h3 className="font-semibold text-neutral-900 mb-2">Subida de fotos</h3>
              <p className="text-sm text-neutral-600">
                El paciente puede subir una foto del examen desde su celular (cámara directa).
              </p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <Brain className="w-8 h-8 text-medical-600 mb-4" />
              <h3 className="font-semibold text-neutral-900 mb-2">Google Vision OCR</h3>
              <p className="text-sm text-neutral-600">
                Extrae el texto de la imagen con DOCUMENT_TEXT_DETECTION en español.
              </p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200">
              <Activity className="w-8 h-8 text-medical-600 mb-4" />
              <h3 className="font-semibold text-neutral-900 mb-2">Análisis con IA</h3>
              <p className="text-sm text-neutral-600">
                DeepSeek interpreta los valores y genera un análisis en lenguaje sencillo.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Stack técnico */}
      <section className="py-16 px-4">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-8 text-center">
            Stack técnico verificado
          </h2>
          <div className="grid md:grid-cols-3 gap-6">
            {[
              { icon: Server, title: 'Backend', items: ['Java 17', 'Spring Boot 3.2.5', 'Spring Security', 'JPA / Hibernate', 'Flyway (V1-V47)', 'JWT + HttpOnly cookies'] },
              { icon: Code2, title: 'Frontend', items: ['Next.js 16 (App Router)', 'React 19', 'TypeScript strict', 'Tailwind CSS 4', 'React Hook Form + Zod', 'Lucide icons'] },
              { icon: Database, title: 'Datos', items: ['PostgreSQL (Neon)', 'Migraciones versionadas', 'Índices optimizados', 'Audit logs', 'Outbox pattern'] },
              { icon: Brain, title: 'IA', items: ['DeepSeek V4 Flash', 'Spring AI integration', 'Prompt engineering', 'Guardrails deterministas', 'Sin análisis de imágenes médicas'] },
              { icon: Shield, title: 'Seguridad', items: ['JWT stateless', 'Cookie HttpOnly + SameSite=None', 'CORS restrictivo', 'Rate limiting', 'Habeas Data compliance'] },
              { icon: Cloud, title: 'Cloud / DevOps', items: ['Render (backend)', 'Vercel (frontend)', 'Docker', 'GitHub Actions', 'CI/CD automatizado'] },
            ].map((group) => (
              <div key={group.title} className="bg-white rounded-xl p-6 border border-neutral-200">
                <group.icon className="w-8 h-8 text-medical-600 mb-4" />
                <h3 className="font-semibold text-neutral-900 mb-3">{group.title}</h3>
                <ul className="space-y-1">
                  {group.items.map((item) => (
                    <li key={item} className="text-sm text-neutral-600 flex items-start gap-2">
                      <CheckCircle2 className="w-4 h-4 text-medical-500 flex-shrink-0 mt-0.5" />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Testing y calidad */}
      <section className="py-16 px-4 bg-neutral-50">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-8 text-center">
            Calidad verificada con pruebas reales
          </h2>
          <div className="grid md:grid-cols-3 gap-6 mb-8">
            <div className="bg-white rounded-xl p-6 border border-neutral-200 text-center">
              <div className="text-4xl font-bold text-medical-600 mb-2">86+</div>
              <p className="text-sm text-neutral-600">tests backend</p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200 text-center">
              <div className="text-4xl font-bold text-medical-600 mb-2">436</div>
              <p className="text-sm text-neutral-600">tests frontend</p>
            </div>
            <div className="bg-white rounded-xl p-6 border border-neutral-200 text-center">
              <div className="text-4xl font-bold text-medical-600 mb-2">E2E</div>
              <p className="text-sm text-neutral-600">Playwright</p>
            </div>
          </div>
          <div className="bg-white rounded-xl p-6 border border-neutral-200">
            <h3 className="font-semibold text-neutral-900 mb-4 flex items-center gap-2">
              <TestTube className="w-5 h-5 text-medical-600" />
              Herramientas de testing
            </h3>
            <div className="flex flex-wrap gap-2">
              {['JUnit 5', 'Mockito', 'Testcontainers', 'Vitest', 'React Testing Library', 'Playwright', 'JaCoCo', 'ESLint'].map((tool) => (
                <span key={tool} className="px-3 py-1 rounded-full bg-neutral-100 text-neutral-700 text-xs">
                  {tool}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Seguridad */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl font-bold text-neutral-900 mb-8 text-center">
            Security by Design
          </h2>
          <p className="text-center text-neutral-600 mb-8 max-w-2xl mx-auto">
            La seguridad no es una capa final de KIN Medical: está distribuida en autenticación, autorización, persistencia y despliegue.
          </p>
          <div className="grid md:grid-cols-2 gap-4">
            {[
              'Autenticación stateless con JWT + cookies HttpOnly',
              'CORS restrictivo a dominios específicos (sin wildcards)',
              'Verificación manual de médicos por ADMIN',
              'Rate limiting en endpoints sensibles',
              'Protección de datos de salud (Habeas Data)',
              'Emails sin datos clínicos (solo avisos)',
              'Auditoría de accesos a documentos clínicos',
              'Aislamiento por propietario de recurso (IDOR)',
            ].map((item) => (
              <div key={item} className="flex gap-3 items-start bg-white rounded-lg p-4 border border-neutral-200">
                <Lock className="w-5 h-5 text-medical-600 flex-shrink-0 mt-0.5" />
                <span className="text-sm text-neutral-700">{item}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Diferenciador */}
      <section className="py-16 px-4 bg-medical-600 text-white">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl font-bold mb-6">Diferenciador clave</h2>
          <div className="grid md:grid-cols-2 gap-6 text-left">
            <div className="bg-white/10 rounded-xl p-6">
              <h3 className="font-semibold text-lg mb-2">KIN Medical</h3>
              <ul className="space-y-2 text-sm text-medical-50">
                <li>✅ Motor determinista decide triaje</li>
                <li>✅ IA solo redacta el resultado</li>
                <li>✅ Fuentes médicas verificables (OMS, CIE-10)</li>
                <li>✅ Sin datos clínicos en emails/WhatsApp</li>
                <li>✅ Verificación manual de médicos</li>
                <li>✅ Accesible WCAG AAA</li>
              </ul>
            </div>
            <div className="bg-white/10 rounded-xl p-6">
              <h3 className="font-semibold text-lg mb-2">Plataformas tradicionales</h3>
              <ul className="space-y-2 text-sm text-medical-50">
                <li>❌ LLM decide (impredecible)</li>
                <li>❌ IA sin trazabilidad</li>
                <li>❌ Fuentes no verificables</li>
                <li>❌ Datos clínicos expuestos</li>
                <li>❌ Verificación automática dudosa</li>
                <li>❌ Contraste insuficiente</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl font-bold text-neutral-900 mb-4">
            ¿Quieres conocer KIN Medical en acción?
          </h2>
          <p className="text-neutral-600 mb-8 max-w-2xl mx-auto">
            La plataforma está disponible en producción. Regístrate gratis y explora todas las funcionalidades.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/register/salud/paciente"
              className="inline-flex items-center justify-center rounded-lg bg-medical-600 px-6 py-3 font-semibold text-white hover:bg-medical-700 transition"
            >
              Soy paciente
            </Link>
            <Link
              href="/register/salud/medico"
              className="inline-flex items-center justify-center rounded-lg border-2 border-medical-600 px-6 py-3 font-semibold text-medical-700 hover:bg-medical-50 transition"
            >
              Soy médico
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
