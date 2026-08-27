import { ARCH_QUALITY } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "Engineering Quality": métricas de pruebas del proyecto. Los valores
 * se verifican contra la suite del repositorio y se actualizan con cada
 * ejecución. Son evidencia de ingeniería, no decoración.
 */
export default function QualitySection() {
  return (
    <section
      id="engineering-quality"
      aria-labelledby="engineering-quality-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_QUALITY.eyebrow}
          title={ARCH_QUALITY.title}
          intro={ARCH_QUALITY.intro}
        />

        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-2 gap-4 lg:grid-cols-4">
          {ARCH_QUALITY.metrics.map((metric) => (
            <div
              key={metric.label}
              className="rounded-2xl border border-neutral-200 bg-white p-6 text-center shadow-sm"
            >
              <p className="text-3xl font-extrabold tracking-tight text-primary-700 sm:text-4xl">
                {metric.value}
              </p>
              <p className="mt-2 text-sm font-semibold text-neutral-900">{metric.label}</p>
              {metric.note && <p className="mt-1.5 text-xs leading-5 text-neutral-500">{metric.note}</p>}
            </div>
          ))}
        </div>

        <div className="mx-auto mt-10 max-w-5xl">
          <p className="text-center text-xs font-semibold uppercase tracking-widest text-neutral-400">
            Herramientas de testing y calidad
          </p>
          <ul
            className="mx-auto mt-5 flex max-w-4xl flex-wrap items-center justify-center gap-2.5"
            role="list"
          >
            {ARCH_QUALITY.tools.map((tool) => (
              <li
                key={tool}
                className="rounded-full border border-neutral-200 bg-white px-4 py-1.5 text-sm font-medium text-neutral-700 shadow-sm"
              >
                {tool}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
