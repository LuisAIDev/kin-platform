import { ARCH_SECURITY } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "Security by Design": controles de seguridad distribuidos en toda la
 * arquitectura y el flujo del guard de conexiones externas. Solo se mencionan
 * medidas y principios, nunca valores privados.
 */
export default function SecuritySection() {
  const { measures, ssrf, connectionFlow } = ARCH_SECURITY;
  const lastIndex = connectionFlow.length - 1;

  return (
    <section
      id="seguridad"
      aria-labelledby="seguridad-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_SECURITY.eyebrow}
          title={ARCH_SECURITY.title}
          intro={ARCH_SECURITY.intro}
        />

        <ul
          className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"
          role="list"
        >
          {measures.map((measure) => (
            <li
              key={measure}
              className="flex items-start gap-3 rounded-xl border border-neutral-200 bg-white px-4 py-3.5 shadow-sm"
            >
              <svg
                className="mt-0.5 h-5 w-5 shrink-0 text-primary-600"
                viewBox="0 0 20 20"
                fill="currentColor"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M10 1a4.5 4.5 0 0 0-4.5 4.5V9H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2h-.5V5.5A4.5 4.5 0 0 0 10 1Zm3 8V5.5a3 3 0 1 0-6 0V9h6Z"
                  clipRule="evenodd"
                />
              </svg>
              <span className="text-sm font-medium leading-6 text-neutral-700">{measure}</span>
            </li>
          ))}
        </ul>

        <div className="mx-auto mt-12 grid max-w-6xl grid-cols-1 gap-10 lg:grid-cols-2 lg:items-start">
          <aside className="rounded-2xl border border-primary-100 bg-primary-50/50 p-6 sm:p-8">
            <h3 className="text-base font-semibold text-neutral-900">
              Protección contra SSRF, en términos simples
            </h3>
            <p className="mt-4 text-sm leading-6 text-neutral-600">{ssrf}</p>
            <p className="mt-4 border-t border-primary-100 pt-4 text-xs leading-5 text-neutral-500">
              KIN es offline-first: sin una política de fuentes explícita, el sistema no
              realiza llamadas externas. Los secretos se gestionan por variables de
              entorno y nunca se exponen públicamente.
            </p>
          </aside>

          <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
            <h3 className="text-base font-semibold text-neutral-900">
              Cómo se protege cada conexión externa
            </h3>
            <ol className="mt-6 flex flex-col" role="list">
              {connectionFlow.map((step, index) => (
                <li key={step.label}>
                  <div className="flex items-start gap-3">
                    <span
                      className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-[11px] font-bold text-primary-700 ring-1 ring-primary-100"
                      aria-hidden="true"
                    >
                      {index + 1}
                    </span>
                    <div>
                      <p className="text-sm font-semibold text-neutral-900">{step.label}</p>
                      <p className="mt-0.5 text-xs leading-5 text-neutral-500">
                        {step.description}
                      </p>
                    </div>
                  </div>
                  {index !== lastIndex && (
                    <div className="ml-3.5 border-l-2 border-neutral-100 py-2" aria-hidden="true" />
                  )}
                </li>
              ))}
            </ol>
          </div>
        </div>
      </div>
    </section>
  );
}
