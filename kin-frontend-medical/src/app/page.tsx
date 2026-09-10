import Link from "next/link";

const PHYSICIAN_FEATURES = [
  {
    title: "Triaje digital con IA asistencial",
    description:
      "Analiza síntomas, prioriza por urgencia y apoya la decisión clínica con verificación OMS.",
  },
  {
    title: "Portal Médico",
    description:
      "Gestiona tu cartera de pacientes, resúmenes clínicos, alertas e invitaciones en un solo lugar.",
  },
  {
    title: "Seguimiento de pacientes",
    description:
      "Planes de cuidado con tareas, evolución y recordatorios para no perder el hilo clínico.",
  },
  {
    title: "Agenda y disponibilidad",
    description:
      "Configura horarios, publica slots y confirma citas con recordatorios automáticos.",
  },
  {
    title: "Documentos clínicos compartidos",
    description:
      "Sube, clasifica y consulta documentos con trazabilidad y auditoría de accesos.",
  },
  {
    title: "Telemedicina",
    description:
      "Mensajería cifrada y gestión de citas para acompañar al paciente entre consultas.",
  },
];

const CLINIC_FEATURES = [
  "Múltiples médicos por institución",
  "Múltiples sedes",
  "Dashboards institucionales",
  "Reportes regulatorios",
];

const IPS_FEATURES = [
  "Gestión de redes de prestadores",
  "Autorizaciones y flujos de referencia",
  "Reportes regulatorios",
];

const PLANS = [
  {
    name: "Medical Free",
    price: "$0",
    period: "/mes",
    description: "Para empezar a digitalizar tu práctica.",
    features: ["3 pacientes", "10 triajes/mes", "Portal Médico básico"],
    cta: "Comenzar gratis",
    href: "/register/salud/medico",
    highlighted: false,
  },
  {
    name: "Medical Pro",
    price: "$29",
    period: "/mes",
    description: "Para médicos independientes con práctica activa.",
    features: ["100 pacientes", "Triajes ilimitados", "Seguimiento y agenda", "Telemedicina"],
    cta: "Solicitar acceso",
    href: "/register/salud/medico",
    highlighted: true,
  },
  {
    name: "Medical Clinic",
    price: "$299",
    period: "/mes",
    description: "Para clínicas y equipos médicos.",
    features: ["Multi-médico", "Multi-sede", "Dashboards institucionales"],
    cta: "Próximamente",
    href: "#contacto",
    highlighted: false,
  },
];

export default function HomePage() {
  return (
    <div className="min-h-screen bg-white text-neutral-900">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-neutral-200 bg-white/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-medical-600">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <path d="M9 12l2 2 4-4" />
              </svg>
            </div>
            <span className="text-lg font-bold tracking-tight">KIN Medical</span>
          </Link>
          <nav className="hidden items-center gap-8 md:flex">
            <a href="#medicos" className="text-sm font-medium text-neutral-500 hover:text-primary-600">Para médicos</a>
            <a href="#clinicas" className="text-sm font-medium text-neutral-500 hover:text-primary-600">Para clínicas</a>
            <a href="#ips" className="text-sm font-medium text-neutral-500 hover:text-primary-600">IPS/EPS</a>
            <a href="#planes" className="text-sm font-medium text-neutral-500 hover:text-primary-600">Planes</a>
          </nav>
          <div className="flex items-center gap-3">
            <Link href="/login" className="rounded-lg px-3 py-2 text-sm font-medium text-neutral-600 hover:text-primary-600">
              Iniciar sesión
            </Link>
            <Link href="/register" className="btn-primary">Crear cuenta</Link>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="mx-auto max-w-6xl px-4 py-20 text-center sm:px-6 sm:py-28 lg:px-8">
        <span className="inline-flex items-center rounded-full bg-medical-50 px-3 py-1 text-xs font-semibold text-medical-700">
          Plataforma clínica para médicos, clínicas y redes de atención
        </span>
        <h1 className="mx-auto mt-6 max-w-3xl text-4xl font-bold tracking-tight text-balance sm:text-5xl">
          KIN Medical: infraestructura inteligente para tu práctica clínica.
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-neutral-600">
          Gestiona pacientes, agenda, seguimiento y telemedicina en un único lugar.
        </p>
        <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
          <Link href="/register/salud/medico" className="btn-primary px-6 py-3">Soy Médico</Link>
          <Link href="/register/salud/paciente" className="btn-secondary px-6 py-3">Soy Paciente</Link>
          <a href="#contacto" className="btn-secondary px-6 py-3">Quiero para mi Clínica</a>
        </div>
      </section>

      {/* Sección 1 — Médicos independientes */}
      <section id="medicos" className="border-t border-neutral-100 bg-neutral-50">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
          <div className="max-w-2xl">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">Tu práctica clínica, organizada e inteligente.</h2>
            <p className="mt-4 text-lg text-neutral-600">
              Todo lo que necesitas para atender mejor, con menos carga administrativa.
            </p>
          </div>
          <div className="mt-12 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {PHYSICIAN_FEATURES.map((f) => (
              <div key={f.title} className="card">
                <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-medical-100 text-medical-700">
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <h3 className="text-base font-semibold">{f.title}</h3>
                <p className="mt-2 text-sm text-neutral-600">{f.description}</p>
              </div>
            ))}
          </div>
          <div className="mt-10">
            <Link href="/register/salud/medico" className="btn-primary px-6 py-3">Solicitar acceso como profesional</Link>
          </div>
        </div>
      </section>

      {/* Sección 2 — Clínicas y hospitales */}
      <section id="clinicas" className="border-t border-neutral-100">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 gap-12 lg:grid-cols-2 lg:items-center">
            <div>
              <span className="inline-flex items-center rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700">
                Próximamente
              </span>
              <h2 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">¿Tienes una clínica o hospital?</h2>
              <p className="mt-4 text-lg text-neutral-600">
                Estamos preparando KIN Medical para equipos médicos: multi-médico, multi-sede,
                dashboards institucionales y gestión de equipos.
              </p>
              <ul className="mt-6 space-y-3">
                {CLINIC_FEATURES.map((f) => (
                  <li key={f} className="flex items-center gap-3 text-neutral-700">
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary-100 text-primary-700">
                      <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                    </span>
                    {f}
                  </li>
                ))}
              </ul>
              <a href="#contacto" className="btn-primary mt-8 px-6 py-3">Quiero información</a>
            </div>
            <div className="rounded-2xl border border-neutral-200 bg-neutral-50 p-8">
              <div className="grid grid-cols-2 gap-4">
                {["Multi-médico", "Multi-sede", "Dashboards", "Reportes"].map((t) => (
                  <div key={t} className="rounded-xl border border-neutral-200 bg-white p-4 text-center">
                    <div className="mx-auto mb-2 flex h-9 w-9 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                      </svg>
                    </div>
                    <span className="text-sm font-medium text-neutral-700">{t}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Sección 3 — IPS/EPS */}
      <section id="ips" className="border-t border-neutral-100 bg-primary-950 text-white">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
          <div className="max-w-2xl">
            <span className="inline-flex items-center rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-primary-100">
              Futuro
            </span>
            <h2 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">KIN Medical para redes de atención.</h2>
            <p className="mt-4 text-lg text-primary-100">
              Diseñado para IPS y EPS: gestión de redes de prestadores, autorizaciones y reportes regulatorios.
            </p>
          </div>
          <ul className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-3">
            {IPS_FEATURES.map((f) => (
              <li key={f} className="rounded-xl border border-white/10 bg-white/5 p-5 text-sm text-primary-50">
                {f}
              </li>
            ))}
          </ul>
          <a href="#contacto" className="mt-8 inline-flex items-center justify-center rounded-lg bg-white px-6 py-3 text-sm font-semibold text-primary-800 hover:bg-primary-50">
            Más información
          </a>
        </div>
      </section>

      {/* Planes */}
      <section id="planes" className="border-t border-neutral-100">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">Planes para cada etapa</h2>
            <p className="mt-4 text-lg text-neutral-600">Empieza gratis y crece cuando tu práctica lo necesite.</p>
          </div>
          <div className="mt-12 grid grid-cols-1 gap-6 lg:grid-cols-3">
            {PLANS.map((plan) => (
              <div
                key={plan.name}
                className={`rounded-2xl border p-8 ${plan.highlighted ? "border-medical-500 bg-white shadow-soft ring-1 ring-medical-500" : "border-neutral-200 bg-white"}`}
              >
                {plan.highlighted && (
                  <span className="mb-3 inline-block rounded-full bg-medical-100 px-3 py-1 text-xs font-semibold text-medical-700">
                    Más popular
                  </span>
                )}
                <h3 className="text-lg font-semibold">{plan.name}</h3>
                <p className="mt-1 text-sm text-neutral-500">{plan.description}</p>
                <div className="mt-6 flex items-baseline gap-1">
                  <span className="text-4xl font-bold">{plan.price}</span>
                  <span className="text-neutral-500">{plan.period}</span>
                </div>
                <ul className="mt-6 space-y-3">
                  {plan.features.map((f) => (
                    <li key={f} className="flex items-center gap-2 text-sm text-neutral-700">
                      <svg className="h-4 w-4 text-medical-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                      {f}
                    </li>
                  ))}
                </ul>
                <Link
                  href={plan.href}
                  className={`mt-8 block w-full rounded-lg px-4 py-3 text-center text-sm font-semibold transition ${plan.highlighted ? "bg-medical-600 text-white hover:bg-medical-700" : "border border-neutral-300 text-neutral-700 hover:bg-neutral-50"}`}
                >
                  {plan.cta}
                </Link>
              </div>
            ))}
          </div>
          <div className="mt-10 text-center">
            <Link href="/dashboard/physician/plans" className="text-sm font-semibold text-primary-600 hover:text-primary-700">
              Ver planes completos →
            </Link>
          </div>
        </div>
      </section>

      {/* Contacto */}
      <section id="contacto" className="border-t border-neutral-100 bg-neutral-50">
        <div className="mx-auto max-w-3xl px-4 py-20 text-center sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">Hablemos de tu clínica o red de atención</h2>
          <p className="mt-4 text-lg text-neutral-600">
            Cuéntanos tu caso y te contactamos para explorar cómo KIN Medical puede ayudarte.
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
            <a
              href="mailto:clinicas@kin-platform-medical.com?subject=Quiero%20informaci%C3%B3n%20sobre%20KIN%20Medical"
              className="btn-primary px-6 py-3"
            >
              Escribir a clinicas@kin-platform-medical.com
            </a>
            <Link href="/register" className="btn-secondary px-6 py-3">Crear cuenta</Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-neutral-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="flex flex-col items-start justify-between gap-8 sm:flex-row sm:items-center">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-medical-600">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  <path d="M9 12l2 2 4-4" />
                </svg>
              </div>
              <span className="text-lg font-bold tracking-tight">KIN Medical</span>
            </div>
            <nav className="flex flex-wrap items-center gap-6 text-sm text-neutral-500">
              <a href="#medicos" className="hover:text-primary-600">Sobre KIN Medical</a>
              <a href="#contacto" className="hover:text-primary-600">Contacto</a>
              <a href="/terminos" className="hover:text-primary-600">Términos</a>
              <a href="/privacidad" className="hover:text-primary-600">Privacidad</a>
            </nav>
          </div>
          <div className="mt-8 border-t border-neutral-100 pt-6 text-xs text-neutral-400">
            © {new Date().getFullYear()} KIN Medical. Todos los derechos reservados.
          </div>
        </div>
      </footer>
    </div>
  );
}