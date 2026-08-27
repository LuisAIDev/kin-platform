import {
  ARCH_PRINCIPLE,
  ARCH_PRINCIPLE_BOUNDARY,
  ARCH_PRINCIPLE_OUTCOMES,
} from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

function FlowConnector() {
  return (
    <div className="flex justify-center py-1" aria-hidden="true">
      <svg
        className="h-4 w-4 text-primary-400"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
      >
        <path strokeLinecap="round" strokeLinejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
      </svg>
    </div>
  );
}

/**
 * Sección destacada "Java decide. El LLM únicamente comunica." — el principio
 * arquitectónico central de KIN. Incluye el flujo conceptual completo:
 * del usuario al resultado, con la política determinista en el centro.
 */
export default function PrincipleSection() {
  const { items, flow } = ARCH_PRINCIPLE;
  const lastIndex = flow.length - 1;

  return (
    <section
      id="principio"
      aria-labelledby="principio-title"
      className="relative overflow-hidden border-y border-primary-100 bg-gradient-to-br from-primary-50/70 via-white to-accent-50/30 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_PRINCIPLE.eyebrow}
          title={ARCH_PRINCIPLE.title}
          intro={ARCH_PRINCIPLE.intro}
        />

        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-1 gap-6 sm:grid-cols-2">
          {items.map((item) => (
            <article
              key={item.title}
              className="rounded-2xl border border-primary-100 bg-white p-6 shadow-sm sm:p-8"
            >
              <h3 className="flex items-center gap-2.5 text-base font-semibold text-neutral-900">
                <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary-500" aria-hidden="true" />
                {item.title}
              </h3>
              <p className="mt-3 text-sm leading-6 text-neutral-500">{item.description}</p>
            </article>
          ))}
        </div>

        <div className="mx-auto mt-14 max-w-3xl">
          <p className="text-center text-xs font-semibold uppercase tracking-widest text-neutral-400">
            El flujo que sigue cada turno
          </p>
          <ol className="mt-6 flex flex-col" role="list">
            {flow.map((step, index) => (
              <li key={step.label}>
                <div className="flex items-start gap-3 rounded-xl border border-primary-100 bg-white px-4 py-3 shadow-sm">
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
                {index !== lastIndex && <FlowConnector />}
              </li>
            ))}
          </ol>
        </div>

        <div className="mx-auto mt-12 max-w-3xl">
          <p className="text-center text-xs font-semibold uppercase tracking-widest text-neutral-400">
            El límite de la IA
          </p>
          <ul
            className="mx-auto mt-4 flex max-w-3xl flex-wrap items-center justify-center gap-2.5"
            role="list"
          >
            {ARCH_PRINCIPLE_BOUNDARY.map((boundary) => (
              <li
                key={boundary}
                className="rounded-full border border-primary-100 bg-white px-4 py-1.5 text-sm font-medium text-primary-700 shadow-sm"
              >
                {boundary}
              </li>
            ))}
          </ul>
        </div>

        <div className="mx-auto mt-12 max-w-5xl">
          <p className="text-center text-xs font-semibold uppercase tracking-widest text-neutral-400">
            Esto busca producir resultados
          </p>
          <ul
            className="mx-auto mt-5 flex max-w-4xl flex-wrap items-center justify-center gap-2.5"
            role="list"
          >
            {ARCH_PRINCIPLE_OUTCOMES.map((outcome) => (
              <li
                key={outcome}
                className="rounded-full border border-primary-100 bg-white px-4 py-1.5 text-sm font-medium text-primary-700 shadow-sm"
              >
                {outcome}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
