import { ARCH_DEVSECOPS } from "./architectureContent";
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
 * Sección "DevSecOps & CI/CD": flujo de integración continua con puertas de
 * calidad y seguridad. Se listan herramientas verificadas, sin configuraciones.
 */
export default function DevSecOpsSection() {
  const { flow, tools } = ARCH_DEVSECOPS;
  const lastIndex = flow.length - 1;

  return (
    <section
      id="devsecops"
      aria-labelledby="devsecops-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_DEVSECOPS.eyebrow}
          title={ARCH_DEVSECOPS.title}
          intro={ARCH_DEVSECOPS.intro}
        />

        <div className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-10 lg:grid-cols-2 lg:items-start">
          <ol className="mx-auto w-full max-w-md" role="list">
            {flow.map((step, index) => (
              <li key={step.label}>
                <div className="rounded-xl border border-neutral-200 bg-white px-5 py-4 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span
                      className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-xs font-bold text-primary-700 ring-1 ring-primary-100"
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
                </div>
                {index !== lastIndex && <FlowConnector />}
              </li>
            ))}
          </ol>

          <aside className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
            <h3 className="text-base font-semibold text-neutral-900">
              Herramientas de calidad y seguridad integradas
            </h3>
            <ul className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2" role="list">
              {tools.map((tool) => (
                <li
                  key={tool}
                  className="rounded-lg border border-neutral-100 bg-neutral-50/60 px-4 py-3 text-sm font-medium text-neutral-800"
                >
                  {tool}
                </li>
              ))}
            </ul>
            <p className="mt-6 border-t border-neutral-100 pt-5 text-xs leading-5 text-neutral-500">
              Los detalles de configuración de los workflows y las reglas internas no se
              exponen públicamente.
            </p>
          </aside>
        </div>
      </div>
    </section>
  );
}
