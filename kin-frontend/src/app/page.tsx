import Link from "next/link";
import Navbar from "@/components/layout/Navbar";
import PricingSection from "@/components/pricing/PricingSection";

export default function Home() {
  return (
    <>
      <Navbar />

      {/* ── Hero ─────────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-primary-50/80 via-white to-accent-50/50" />
        <div
          className="pointer-events-none absolute -top-40 right-0 -z-10 transform-gpu blur-3xl"
          aria-hidden="true"
        >
          <div
            className="aspect-[1155/678] w-[72.1875rem] bg-gradient-to-tr from-primary-200 to-accent-200 opacity-30"
            style={{
              clipPath:
                "polygon(74.1% 44.1%, 100% 61.6%, 97.5% 26.9%, 85.5% 0.1%, 80.7% 2%, 72.5% 32.5%, 60.2% 62.4%, 52.4% 68.1%, 47.5% 58.3%, 45.2% 34.5%, 27.5% 76.7%, 0.1% 64.9%, 17.9% 100%, 27.6% 76.8%, 76.1% 97.7%, 74.1% 44.1%)",
            }}
          />
        </div>

        {/* Animated decorative blobs */}
        <div className="absolute top-20 -left-40 w-96 h-96 bg-primary-200/20 rounded-full blur-3xl animate-[blob_8s_ease-in-out_infinite]" />
        <div className="absolute top-1/3 -right-32 w-[30rem] h-[30rem] bg-accent-200/15 rounded-full blur-3xl animate-[blob_10s_ease-in-out_infinite_2s]" />
        <div className="absolute -bottom-20 left-1/4 w-80 h-80 bg-primary-100/30 rounded-full blur-3xl animate-[blob_7s_ease-in-out_infinite_4s]" />

        <div className="relative mx-auto max-w-7xl px-4 py-28 sm:px-6 sm:py-40 lg:px-8">
          <div className="mx-auto max-w-4xl text-center">
            {/* Eyebrow */}
            <div className="inline-flex items-center gap-2 rounded-full border border-primary-200 bg-primary-50/80 px-4 py-1.5 text-sm font-medium text-primary-700 mb-6 shadow-sm">
              <span className="h-1.5 w-1.5 rounded-full bg-primary-500 animate-pulse" />
              Knowledge, Innovation &amp; Navigation
            </div>

            {/* Title */}
            <h1 className="text-6xl font-extrabold tracking-tight sm:text-8xl">
              <span className="bg-gradient-to-r from-primary-600 via-primary-500 to-accent-500 bg-clip-text text-transparent">
                KIN
              </span>
            </h1>

            {/* Subtitle */}
            <p className="mt-6 text-lg leading-8 text-neutral-500 sm:text-xl max-w-2xl mx-auto">
              Estructura tu proyecto en menos de 60 minutos con asistencia de IA.
            </p>

            {/* CTAs */}
            <div className="mt-10 flex items-center justify-center gap-4">
              <Link
                href="/register"
                className="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-8 py-3.5 text-sm font-semibold text-white shadow-lg shadow-primary-600/25 hover:shadow-xl hover:shadow-primary-600/30 hover:bg-primary-500 hover:scale-105 active:scale-95 transition-all duration-200"
              >
                Comenzar gratis
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3" />
                </svg>
              </Link>
              <Link
                href="/login"
                className="inline-flex items-center gap-2 rounded-xl border-2 border-neutral-300 bg-white/80 px-8 py-3.5 text-sm font-semibold text-neutral-700 shadow-sm hover:bg-neutral-50 hover:border-neutral-400 hover:shadow-md hover:scale-105 active:scale-95 transition-all duration-200 backdrop-blur-sm"
              >
                Iniciar sesión
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ── Cómo funciona ────────────────────────────────────── */}
      <section className="border-y border-neutral-100 bg-neutral-50/50">
        <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-3">
              Cómo funciona
            </p>
            <h2 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
              De la idea al plan en 3 pasos
            </h2>
          </div>
          <div className="mx-auto mt-14 grid max-w-4xl grid-cols-1 gap-8 sm:grid-cols-3">
            <div className="text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary-100 text-primary-700 text-xl font-bold">
                1
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">Describe tu idea</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                Cuéntanos tu proyecto en tus propias palabras. No necesitas estructura técnica.
              </p>
            </div>
            <div className="text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-accent-100 text-accent-700 text-xl font-bold">
                2
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">La IA estructura tu proyecto</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                Nuestra inteligencia artificial analiza y organiza cada aspecto de tu iniciativa.
              </p>
            </div>
            <div className="text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary-100 text-primary-700 text-xl font-bold">
                3
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">Exporta tu reporte</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                Descarga un PDF profesional con el análisis completo, listo para compartir.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── Características ──────────────────────────────────── */}
      <section id="caracteristicas" className="py-24 sm:py-32">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-3">
              Funcionalidades
            </p>
            <h2 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
              Características clave
            </h2>
            <p className="mt-4 text-lg text-neutral-500">
              Todo lo que necesitas para transformar tu idea en un proyecto estructurado.
            </p>
          </div>

          <div className="mx-auto mt-16 grid max-w-5xl grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {/* Card 1 */}
            <div className="group relative rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-200">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-50 group-hover:bg-primary-100 transition-colors duration-200 ring-1 ring-primary-100">
                <svg className="h-6 w-6 text-primary-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715 18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 0 0-2.455 2.456ZM16.894 20.567 16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" />
                </svg>
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">
                Asistente de IA
              </h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                Genera la estructura completa de tu proyecto con inteligencia artificial. Describe tu idea y obtén un plan detallado al instante.
              </p>
            </div>

            {/* Card 2 */}
            <div className="group relative rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-200">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-50 group-hover:bg-primary-100 transition-colors duration-200 ring-1 ring-primary-100">
                <svg className="h-6 w-6 text-primary-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
                </svg>
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">
                Scoring de Viabilidad
              </h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                Evalúa la factibilidad técnica y comercial de tu proyecto con métricas objetivas y recomendaciones accionables.
              </p>
            </div>

            {/* Card 3 */}
            <div className="group relative rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-200">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-50 group-hover:bg-primary-100 transition-colors duration-200 ring-1 ring-primary-100">
                <svg className="h-6 w-6 text-primary-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                </svg>
              </div>
              <h3 className="mt-6 text-lg font-semibold text-neutral-900">
                Exportación a PDF
              </h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                Descarga reportes profesionales en PDF con el análisis completo de tu proyecto, listos para presentar a inversores.
              </p>
            </div>
          </div>
        </div>
      </section>

      <PricingSection />

      {/* ── Footer ──────────────────────────────────────────── */}
      <footer id="contacto" className="border-t border-neutral-100 bg-neutral-50/50">
        <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 gap-8 sm:grid-cols-3 items-start">
            <div>
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-sm">
                  <span className="text-white font-bold text-sm tracking-tight">K</span>
                </div>
                <span className="text-lg font-bold text-primary-900 tracking-tight">KIN</span>
              </div>
              <p className="mt-3 text-sm text-neutral-500 max-w-xs leading-relaxed">
                Knowledge, Innovation &amp; Navigation. Estructura tu proyecto con IA.
              </p>
            </div>
            <div className="sm:text-center">
              <h4 className="text-sm font-semibold text-neutral-900 mb-4">Producto</h4>
              <ul className="space-y-3">
                <li>
                  <a href="#caracteristicas" className="text-sm text-neutral-500 hover:text-primary-600 transition-colors duration-200">
                    Características
                  </a>
                </li>
                <li>
                  <a href="#precios" className="text-sm text-neutral-500 hover:text-primary-600 transition-colors duration-200">
                    Precios
                  </a>
                </li>
              </ul>
            </div>
            <div className="sm:text-right">
              <h4 className="text-sm font-semibold text-neutral-900 mb-4">Contacto</h4>
              <ul className="space-y-3">
                <li>
                  <a
                    href="mailto:hola@kin.ai"
                    className="text-sm text-neutral-500 hover:text-primary-600 transition-colors duration-200"
                  >
                    hola@kin.ai
                  </a>
                </li>
                <li>
                  <p className="text-sm text-neutral-400">&copy; {new Date().getFullYear()} KIN</p>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </footer>
    </>
  );
}
