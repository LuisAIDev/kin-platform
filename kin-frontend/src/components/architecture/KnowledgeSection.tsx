import { ARCH_KNOWLEDGE } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "Knowledge Engine — conocimiento externo controlado": flujo
 * conceptual de adquisición, principios de seguridad, capacidades del motor y
 * ejemplos conceptuales por categoría. No se exponen la allowlist real, las
 * URLs ni la configuración interna.
 */
export default function KnowledgeSection() {
  const { flow, principles, capabilities, categoryExamples } = ARCH_KNOWLEDGE;
  const lastIndex = flow.length - 1;

  return (
    <section
      id="knowledge-engine"
      aria-labelledby="knowledge-engine-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_KNOWLEDGE.eyebrow}
          title={ARCH_KNOWLEDGE.title}
          intro={ARCH_KNOWLEDGE.intro}
        />

        <div className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-10 lg:grid-cols-2 lg:items-start">
          <div
            className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8"
            aria-label="Flujo del Knowledge Engine"
          >
            <h3 className="text-base font-semibold text-neutral-900">
              Flujo conceptual de adquisición
            </h3>
            <ol className="mt-6 flex flex-col" role="list">
              {flow.map((step, index) => (
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

          <div className="rounded-2xl border border-primary-100 bg-primary-50/50 p-6 sm:p-8">
            <h3 className="text-base font-semibold text-neutral-900">
              Principios de control del conocimiento
            </h3>
            <ul className="mt-6 space-y-4" role="list">
              {principles.map((principle) => (
                <li key={principle} className="flex items-start gap-3">
                  <svg
                    className="mt-0.5 h-5 w-5 shrink-0 text-primary-600"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                    aria-hidden="true"
                  >
                    <path
                      fillRule="evenodd"
                      d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                      clipRule="evenodd"
                    />
                  </svg>
                  <p className="text-sm font-medium leading-6 text-neutral-700">{principle}</p>
                </li>
              ))}
            </ul>
            <p className="mt-6 border-t border-primary-100 pt-5 text-xs leading-5 text-neutral-500">
              La política de fuentes se gestiona de forma controlada y no se expone
              públicamente.
            </p>
          </div>
        </div>

        <div className="mx-auto mt-12 max-w-6xl">
          <h3 className="text-center text-base font-semibold text-neutral-900">
            Capacidades del motor
          </h3>
          <ul
            className="mx-auto mt-6 grid max-w-5xl grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-5"
            role="list"
          >
            {capabilities.map((capability) => (
              <li
                key={capability}
                className="flex items-start gap-2 rounded-xl border border-neutral-200 bg-white px-3.5 py-3 shadow-sm"
              >
                <svg
                  className="mt-0.5 h-4 w-4 shrink-0 text-primary-500"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path
                    fillRule="evenodd"
                    d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                    clipRule="evenodd"
                  />
                </svg>
                <span className="text-xs font-medium leading-5 text-neutral-700">
                  {capability}
                </span>
              </li>
            ))}
          </ul>
        </div>

        <div className="mx-auto mt-12 max-w-6xl">
          <h3 className="text-center text-base font-semibold text-neutral-900">
            Fuentes específicas por categoría
          </h3>
          <p className="mx-auto mt-2 max-w-2xl text-center text-xs leading-5 text-neutral-500">
            Ejemplos conceptuales: cada categoría puede tener fuentes específicas, además
            de fuentes generales. Una fuente puede reutilizarse en varias categorías. La
            asignación exacta es una política interna.
          </p>
          <ul
            className="mx-auto mt-6 grid max-w-5xl grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"
            role="list"
          >
            {categoryExamples.map((example) => (
              <li
                key={example.category}
                className="rounded-xl border border-neutral-200 bg-white px-4 py-3.5 shadow-sm"
              >
                <p className="text-sm font-semibold text-neutral-900">{example.category}</p>
                <p className="mt-1 text-xs leading-5 text-neutral-500">
                  → {example.knowledge}
                </p>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
