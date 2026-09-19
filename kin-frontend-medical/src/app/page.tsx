import Link from "next/link";

const PHYSICIAN_FEATURES = [
  {
    title: "Triaje digital con IA asistencial",
    description:
      "Analiza síntomas, prioriza por urgencia y apoya la decisión clínica con verificación OMS.",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M3 13h2v8H3zM9 8h2v13H9zM15 11h2v10h-2zM21 4h2v17h-2z" />
      </svg>
    ),
    link: "Comenzar →",
  },
  {
    title: "Portal Médico",
    description:
      "Gestiona tu cartera de pacientes, resúmenes clínicos, alertas e invitaciones en un solo lugar.",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
    link: "Comenzar →",
  },
  {
    title: "Seguimiento de pacientes",
    description:
      "Planes de cuidado con tareas, evolución y recordatorios para no perder el hilo clínico.",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
      </svg>
    ),
    link: "Comenzar →",
  },
  {
    title: "Agenda y disponibilidad",
    description:
      "Configura horarios, publica slots y confirma citas con recordatorios automáticos.",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
    ),
    link: "Comenzar →",
  },
  {
    title: "Documentos clínicos compartidos",
    description:
      "Sube, clasifica y consulta documentos con trazabilidad y auditoría de accesos.",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    ),
    link: "Comenzar →",
  },
  {
    title: "Mensajería y citas",
    description:
      "Comunicación segura con tus pacientes y gestión de citas, con avisos por WhatsApp y email.",
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
    ),
    link: "Comenzar →",
  },
];

const CLINIC_FEATURES = [
  "Múltiples médicos por institución",
  "Múltiples sedes",
  "Dashboards institucionales",
  "Reportes regulatorios",
];

const IPS_FEATURES = [
  "Redes de prestadores",
  "Autorizaciones y referencias",
  "Reportes regulatorios",
  "Indicadores poblacionales",
];

const PATIENT_PLANS = [
  {
    name: "Personal Free",
    price: 0,
    period: "/mes",
    description: "Para consultas de triaje ocasionales",
    features: [
      "Hasta 3 triajes por mes",
      "Historial básico",
      "Exportación PDF limitada",
    ],
    cta: "Comenzar gratis",
    href: "/register/salud/paciente",
    highlighted: false,
  },
  {
    name: "Personal Start",
    price: 2,
    period: "/mes",
    description: "Para pacientes ocasionales",
    features: [
      "Hasta 20 triajes por mes",
      "Historial completo",
      "Exportación PDF ilimitada",
      "Soporte por email",
    ],
    cta: "Comenzar",
    href: "/register/salud/paciente",
    highlighted: true,
  },
  {
    name: "Personal+",
    price: 9,
    period: "/mes",
    description: "Para consultas frecuentes",
    features: [
      "Triajes ilimitados al mes",
      "PDF ilimitado",
      "Compartir informes",
      "Asistente IA PRO",
    ],
    cta: "Comenzar",
    href: "/register/salud/paciente",
    highlighted: false,
  },
];

const PLANS = [
  {
    name: "Medical Free",
    price: "$0",
    period: "/mes",
    description: "Para empezar a digitalizar tu consulta.",
    features: ["3 pacientes", "10 triajes/mes", "Portal Médico básico"],
    cta: "Comenzar gratis",
    href: "/register/salud/medico",
    highlighted: false,
  },
  {
    name: "Medical Pro",
    price: "$35",
    period: "/mes",
    description: "Para médicos independientes con consulta activa.",
    features: ["100 pacientes", "Triajes ilimitados", "Seguimiento y agenda", "Mensajería y citas"],
    cta: "Solicitar acceso",
    href: "/register/salud/medico?plan=PROFESSIONAL",
    highlighted: true,
  },
];

export default function HomePage() {
  return (
    <div className="min-h-screen bg-white text-neutral-900">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-neutral-200 bg-white/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-medical-500 to-medical-600">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <path d="M9 12l2 2 4-4" />
              </svg>
            </div>
            <span className="text-lg font-bold tracking-tight">KIN Medical</span>
          </Link>
          <nav className="hidden items-center gap-8 md:flex">
            <a href="#medicos" className="text-sm font-medium text-neutral-500 hover:text-medical-600">Para médicos</a>
            <a href="#clinicas" className="text-sm font-medium text-neutral-500 hover:text-medical-600">Para clínicas</a>
            <a href="#ips" className="text-sm font-medium text-neutral-500 hover:text-medical-600">IPS/EPS</a>
            <a href="#planes" className="text-sm font-medium text-neutral-500 hover:text-medical-600">Planes</a>
            <a href="/sobre" className="text-sm font-medium text-neutral-500 hover:text-medical-600">Sobre KIN Medical</a>
            <a href="/arquitectura" className="text-sm font-medium text-neutral-500 hover:text-medical-600">Arquitectura</a>
          </nav>
          <div className="flex items-center gap-3">
            <Link href="/login" className="rounded-lg px-3 py-2 text-sm font-medium text-neutral-600 hover:text-medical-600">
              Iniciar sesión
            </Link>
            <Link href="/register" className="btn-primary">Crear cuenta</Link>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-medical-50 via-white to-accent-50 py-32 sm:py-40">
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute top-20 left-1/4 h-96 w-96 rounded-full bg-medical-200/30 blur-3xl" />
          <div className="absolute bottom-20 right-1/4 h-96 w-96 rounded-full bg-accent-200/30 blur-3xl" />
        </div>
        <div className="relative mx-auto max-w-6xl px-4 text-center sm:px-6 sm:px-8 lg:px-8">
          <span className="inline-flex items-center rounded-full bg-medical-50 px-3 py-1 text-xs font-semibold text-medical-700 ring-1 ring-medical-100">
            Plataforma clínica para médicos, clínicas y redes de atención
          </span>
          <div className="mt-8 space-y-6">
            <h1 className="text-6xl font-extrabold tracking-tight sm:text-7xl lg:text-8xl bg-gradient-to-r from-medical-600 to-medical-800 bg-clip-text text-transparent">
              KIN Medical
            </h1>
            <h2 className="text-2xl font-semibold text-neutral-700 sm:text-3xl">
              El sistema operativo de la atención médica moderna
            </h2>
            <p className="text-base text-neutral-500 sm:text-lg">
              Para pacientes, médicos, clínicas, IPS, EPS y hospitales
            </p>
          </div>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            <Link href="/register/salud/medico" className="btn-primary px-6 py-3 text-base">Soy Médico</Link>
            <Link href="/register/salud/paciente" className="btn-secondary px-6 py-3 text-base">Soy Paciente</Link>
            <a href="#contacto" className="btn-secondary px-6 py-3 text-base">Quiero para mi Clínica</a>
          </div>
        </div>
      </section>

      {/* Sección 1 — Médicos independientes */}
      <section id="medicos" className="border-t border-neutral-100 py-24 sm:py-32">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-2xl space-y-4">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">Para médicos independientes: tu consulta, potenciada por inteligencia clínica.</h2>
            <p className="text-lg text-neutral-600">
              Todo lo que necesitas para atender mejor, con menos carga administrativa.
            </p>
          </div>
          <div className="mt-12 grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {PHYSICIAN_FEATURES.map((f) => (
              <div key={f.title} className="card">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-medical-50 text-medical-600 ring-1 ring-medical-100">
                  {f.icon}
                </div>
                <h3 className="text-lg font-semibold text-neutral-900">{f.title}</h3>
                <p className="mt-2 text-sm text-neutral-600">{f.description}</p>
                <div className="mt-4">
                  <Link href="/register/salud/medico" className="text-medical-600 font-semibold hover:text-medical-700">
                    {f.link}
                  </Link>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-10">
            <Link href="/register/salud/medico" className="btn-primary px-6 py-3">Solicitar acceso como profesional</Link>
          </div>
        </div>
      </section>

      {/* Sección 2 — Clínicas y hospitales */}
      <section id="clinicas" className="border-t border-neutral-100 bg-neutral-50 py-24 sm:py-32">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 gap-12 lg:grid-cols-2 lg:items-center">
            <div>
              <span className="inline-flex items-center rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700 ring-1 ring-amber-200">
                Próximamente
              </span>
              <h2 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">Para clínicas y hospitales: gestión integral de equipos médicos.</h2>
              <p className="mt-4 text-lg text-neutral-600">
                KIN Medical se prepara para el entorno hospitalario: multi-médico, multi-sede,
                dashboards institucionales, gestión de equipos y trazabilidad clínica completa.
              </p>
              <ul className="mt-6 space-y-3">
                {CLINIC_FEATURES.map((f) => (
                  <li key={f} className="flex items-center gap-3 text-neutral-700">
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-medical-100 text-medical-600">
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
            <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
              <div className="grid grid-cols-2 gap-4">
                {["Multi-médico", "Multi-sede", "Dashboards", "Reportes"].map((t) => (
                  <div key={t} className="rounded-xl border border-neutral-200 bg-neutral-50 p-4 text-center">
                    <div className="mx-auto mb-2 flex h-9 w-9 items-center justify-center rounded-lg bg-medical-50 text-medical-600">
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
      <section id="ips" className="border-t border-neutral-100 bg-gradient-to-br from-medical-950 via-neutral-900 to-neutral-950 py-24 sm:py-32">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-2xl">
            <span className="inline-flex items-center rounded-full bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-200 ring-1 ring-blue-400/20">
              Futuro
            </span>
            <h2 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl text-white">Para IPS y EPS: inteligencia para redes de atención.</h2>
            <p className="mt-4 text-lg text-blue-100">
              Diseñado para gestionar redes de prestadores: autorizaciones, seguimiento poblacional y reportes regulatorios. La base tecnológica para una atención conectada.
            </p>
          </div>
          <ul className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {IPS_FEATURES.map((f) => (
              <li key={f} className="rounded-xl border border-white/10 bg-white/5 p-6 text-sm text-blue-50 backdrop-blur-sm">
                {f}
              </li>
            ))}
          </ul>
          <a href="#contacto" className="mt-8 inline-flex items-center justify-center rounded-lg bg-medical-500 px-6 py-3 text-sm font-semibold text-white hover:bg-medical-600">
            Más información
          </a>
        </div>
      </section>

      {/* Planes */}
      <section id="planes" className="border-t border-neutral-100 py-24 sm:py-32">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
          {/* Planes para Pacientes */}
          <div className="mb-16">
            <div className="mx-auto max-w-2xl text-center mb-8">
              <h3 className="text-2xl font-bold tracking-tight sm:text-3xl text-medical-700">Planes para Pacientes</h3>
              <p className="mt-2 text-lg text-neutral-600">Cuida tu salud con triaje digital, historial y seguimiento personalizado.</p>
            </div>
            <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {PATIENT_PLANS.map((plan) => (
                <div
                  key={plan.name}
                  className={`rounded-2xl border p-8 ${plan.highlighted ? "border-medical-500 bg-white shadow-xl ring-1 ring-medical-500" : "border-neutral-200 bg-white"} hover:shadow-xl hover:-translate-y-1 transition-all duration-200`}
                >
                  {plan.highlighted && (
                    <span className="mb-3 inline-block rounded-full bg-medical-100 px-3 py-1 text-xs font-semibold text-medical-700">
                      Más popular
                    </span>
                  )}
                  <h3 className="text-xl font-semibold">{plan.name}</h3>
                  <p className="mt-1 text-sm text-neutral-500">{plan.description}</p>
                  <div className="mt-6 flex items-baseline gap-1">
                    <span className="text-4xl font-bold">${plan.price}</span>
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
          </div>

          {/* Planes para Médicos */}
          <div>
            <div className="mx-auto max-w-2xl text-center mb-8">
              <h3 className="text-2xl font-bold tracking-tight sm:text-3xl text-medical-700">Planes para Médicos</h3>
              <p className="mt-2 text-lg text-neutral-600">Digitaliza tu consulta con herramientas clínicas modernas.</p>
            </div>
            <div className="mt-12 grid grid-cols-1 gap-8 md:grid-cols-2 max-w-3xl mx-auto">
              {PLANS.map((plan) => (
                <div
                  key={plan.name}
                  className={`rounded-2xl border p-8 ${plan.highlighted ? "border-medical-500 bg-white shadow-xl ring-1 ring-medical-500" : "border-neutral-200 bg-white"} hover:shadow-xl hover:-translate-y-1 transition-all duration-200`}
                >
                  {plan.highlighted && (
                    <span className="mb-3 inline-block rounded-full bg-medical-100 px-3 py-1 text-xs font-semibold text-medical-700">
                      Más popular
                    </span>
                  )}
                  <h3 className="text-xl font-semibold">{plan.name}</h3>
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
          </div>
        </div>
      </section>

      {/* Contacto */}
      <section id="contacto" className="border-t border-neutral-100 bg-neutral-50 py-24 sm:py-32">
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
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

      {/* Visión */}
      <section className="border-t border-neutral-100 py-24 sm:py-32">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold tracking-tight text-balance sm:text-4xl">
            Una sola plataforma, toda la red de atención.
          </h2>
          <p className="mx-auto mt-4 max-w-3xl text-lg text-neutral-600">
            KIN Medical conecta médicos, clínicas, hospitales, IPS y EPS en una infraestructura
            digital común. Desde la primera consulta hasta la gestión poblacional, todo en un mismo
            sistema.
          </p>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-neutral-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="flex flex-col items-start justify-between gap-8 sm:flex-row sm:items-center">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-medical-500 to-medical-600">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  <path d="M9 12l2 2 4-4" />
                </svg>
              </div>
              <span className="text-lg font-bold tracking-tight">KIN Medical</span>
            </div>
            <nav className="flex flex-wrap items-center gap-6 text-sm text-neutral-500">
              <a href="#medicos" className="hover:text-medical-600">Sobre KIN Medical</a>
              <a href="#contacto" className="hover:text-medical-600">Contacto</a>
              <a href="/terminos" className="hover:text-medical-600">Términos</a>
              <a href="/privacidad" className="hover:text-medical-600">Privacidad</a>
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