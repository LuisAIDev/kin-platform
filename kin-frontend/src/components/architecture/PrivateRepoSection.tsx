import { ARCH_PRIVATE_REPO } from "./architectureContent";

/**
 * Sección "¿Quieres revisar el código?": explica la política de acceso al
 * repositorio privado y dirige al mecanismo de contacto existente. No crea
 * acceso público ni modifica la privacidad del repositorio.
 */
export default function PrivateRepoSection() {
  return (
    <section
      id="acceso-al-codigo"
      aria-labelledby="acceso-al-codigo-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
        <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-3">
          {ARCH_PRIVATE_REPO.eyebrow}
        </p>
        <h2
          id="acceso-al-codigo-title"
          className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl"
        >
          {ARCH_PRIVATE_REPO.title}
        </h2>
        <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-neutral-500 sm:text-lg">
          {ARCH_PRIVATE_REPO.description}
        </p>
        <div className="mt-9">
          <a
            href={ARCH_PRIVATE_REPO.ctaHref}
            className="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-8 py-3.5 text-sm font-semibold text-white shadow-lg shadow-primary-600/25 transition-all duration-200 hover:bg-primary-500 hover:shadow-xl hover:scale-105 active:scale-95"
          >
            {ARCH_PRIVATE_REPO.ctaLabel}
            <svg
              className="h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M6 12 3.269 3.125A59.769 59.769 0 0 1 21.485 12 59.768 59.768 0 0 1 3.27 20.875L5.999 12Zm0 0h7.5"
              />
            </svg>
          </a>
          <p className="mt-4 text-xs leading-5 text-neutral-400">
            La plataforma pública está disponible para evaluar el producto en producción.
          </p>
        </div>
      </div>
    </section>
  );
}
