import Link from "next/link";
import Navbar from "@/components/layout/Navbar";

export default function Home() {
  return (
    <>
      <Navbar />

      {/* ── Hero ─────────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-50 via-white to-purple-50" />
        <div className="pointer-events-none absolute -top-40 right-0 -z-10 transform-gpu blur-3xl" aria-hidden="true">
          <div className="aspect-[1155/678] w-[72.1875rem] bg-gradient-to-tr from-indigo-200 to-purple-200 opacity-30"
            style={{ clipPath: "polygon(74.1% 44.1%, 100% 61.6%, 97.5% 26.9%, 85.5% 0.1%, 80.7% 2%, 72.5% 32.5%, 60.2% 62.4%, 52.4% 68.1%, 47.5% 58.3%, 45.2% 34.5%, 27.5% 76.7%, 0.1% 64.9%, 17.9% 100%, 27.6% 76.8%, 76.1% 97.7%, 74.1% 44.1%)" }}
          />
        </div>
        <div className="relative mx-auto max-w-7xl px-4 py-24 sm:px-6 sm:py-32 lg:px-8">
          <div className="mx-auto max-w-3xl text-center">
            <h1 className="text-5xl font-bold tracking-tight text-neutral-900 sm:text-7xl">
              KIN
            </h1>
            <p className="mt-6 text-lg leading-8 text-neutral-600 sm:text-xl">
              <span className="font-semibold text-neutral-900">K</span>nowledge,{" "}
              <span className="font-semibold text-neutral-900">I</span>nnovation &{" "}
              <span className="font-semibold text-neutral-900">N</span>avigation
            </p>
            <p className="mt-4 text-base text-neutral-500 sm:text-lg">
              Estructura tu proyecto en menos de 60 minutos con asistencia de IA.
            </p>
            <div className="mt-10 flex items-center justify-center gap-4">
              <Link
                href="/register"
                className="rounded-xl bg-neutral-900 px-8 py-3.5 text-sm font-semibold text-white shadow-sm hover:bg-neutral-800 transition"
              >
                Comenzar gratis
              </Link>
              <Link
                href="/login"
                className="rounded-xl border border-neutral-300 px-8 py-3.5 text-sm font-semibold text-neutral-900 hover:bg-neutral-100 transition"
              >
                Iniciar sesión
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ── Características ──────────────────────────────────── */}
      <section id="características" className="py-24 sm:py-32">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
              Características clave
            </h2>
            <p className="mt-2 text-lg text-neutral-600">
              Todo lo que necesitas para transformar tu idea en un proyecto estructurado.
            </p>
          </div>

          <div className="mx-auto mt-16 grid max-w-5xl grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {/* Card 1 */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50">
                <svg className="h-6 w-6 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715 18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 0 0-2.455 2.456ZM16.894 20.567 16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" />
                </svg>
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">
                Asistente de IA
              </h3>
              <p className="mt-2 text-sm leading-6 text-neutral-600">
                Genera la estructura completa de tu proyecto con inteligencia artificial. Describe tu idea y obtén un plan detallado al instante.
              </p>
            </div>

            {/* Card 2 */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50">
                <svg className="h-6 w-6 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
                </svg>
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">
                Scoring de Viabilidad
              </h3>
              <p className="mt-2 text-sm leading-6 text-neutral-600">
                Evalúa la factibilidad técnica y comercial de tu proyecto con métricas objetivas y recomendaciones accionables.
              </p>
            </div>

            {/* Card 3 */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50">
                <svg className="h-6 w-6 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                </svg>
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">
                Exportación a PDF
              </h3>
              <p className="mt-2 text-sm leading-6 text-neutral-600">
                Descarga reportes profesionales en PDF con el análisis completo de tu proyecto, listos para presentar a inversores.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── Precios ──────────────────────────────────────────── */}
      <section id="precios" className="bg-neutral-50 py-24 sm:py-32">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
              Planes de Precios
            </h2>
            <p className="mt-2 text-lg text-neutral-600">
              Elige el plan que mejor se adapte a tu flujo de trabajo.
            </p>
          </div>

          <div className="mx-auto mt-16 grid max-w-4xl grid-cols-1 gap-8 lg:grid-cols-2">
            {/* Plan Básico */}
            <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
              <h3 className="text-lg font-semibold text-neutral-900">Básico Gratis</h3>
              <p className="mt-4">
                <span className="text-5xl font-bold tracking-tight text-neutral-900">$0</span>
                <span className="ml-1 text-sm text-neutral-500">/mes</span>
              </p>
              <p className="mt-1 text-sm text-neutral-500">Para empezar sin compromiso.</p>
              <ul className="mt-8 space-y-4" role="list">
                {["Hasta 3 proyectos", "Asistente de IA básico", "Scoring de viabilidad", "Exportación a PDF"].map((item) => (
                  <li key={item} className="flex items-center gap-3 text-sm text-neutral-700">
                    <svg className="h-5 w-5 shrink-0 text-indigo-600" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z" clipRule="evenodd" />
                    </svg>
                    {item}
                  </li>
                ))}
              </ul>
              <Link
                href="/register"
                className="mt-8 flex w-full items-center justify-center rounded-xl border border-neutral-300 px-6 py-3 text-sm font-semibold text-neutral-900 hover:bg-neutral-50 transition"
              >
                Comenzar gratis
              </Link>
            </div>

            {/* Plan Premium */}
            <div className="relative rounded-2xl border-2 border-indigo-600 bg-white p-8 shadow-md">
              <span className="absolute -top-3 left-6 rounded-full bg-indigo-600 px-4 py-1 text-xs font-semibold text-white">
                Más popular
              </span>
              <h3 className="text-lg font-semibold text-neutral-900">Premium Pro</h3>
              <p className="mt-4">
                <span className="text-5xl font-bold tracking-tight text-neutral-900">$19</span>
                <span className="ml-1 text-sm text-neutral-500">/mes</span>
              </p>
              <p className="mt-1 text-sm text-neutral-500">Para profesionales y equipos.</p>
              <ul className="mt-8 space-y-4" role="list">
                {["Proyectos ilimitados", "IA avanzada con contexto extendido", "Scoring detallado con métricas", "Exportación PDF premium", "Soporte prioritario 24/7"].map((item) => (
                  <li key={item} className="flex items-center gap-3 text-sm text-neutral-700">
                    <svg className="h-5 w-5 shrink-0 text-indigo-600" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z" clipRule="evenodd" />
                    </svg>
                    {item}
                  </li>
                ))}
              </ul>
              <Link
                href="/register"
                className="mt-8 flex w-full items-center justify-center rounded-xl bg-indigo-600 px-6 py-3 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 transition"
              >
                Ir a Premium
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ── Contacto / Footer ────────────────────────────────── */}
      <footer id="contacto" className="border-t border-neutral-200 py-12">
        <div className="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
          <p className="text-sm text-neutral-500">
            &copy; {new Date().getFullYear()} KIN — Knowledge, Innovation &amp; Navigation.
          </p>
          <p className="mt-1 text-sm text-neutral-400">
            Contacto: hola@kin.ai
          </p>
        </div>
      </footer>
    </>
  );
}
