import { ARCH_PIPELINE } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "Motor de inteligencia": pipeline determinista de 16 etapas.
 * La cifra se verificó en la configuración del pipeline (código del proyecto).
 */
export default function PipelineSection() {
  const { count, stages } = ARCH_PIPELINE;

  return (
    <section
      id="motor-de-inteligencia"
      aria-labelledby="motor-de-inteligencia-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_PIPELINE.eyebrow}
          title={ARCH_PIPELINE.title}
          intro={ARCH_PIPELINE.intro}
        />

        <div className="mx-auto mt-12 flex max-w-2xl flex-col items-center gap-2 rounded-2xl border border-primary-100 bg-white p-6 text-center shadow-sm">
          <p className="text-5xl font-extrabold tracking-tight text-primary-700">{count}</p>
          <p className="text-base font-semibold text-neutral-900">etapas de procesamiento</p>
          <p className="mt-1 max-w-lg text-xs leading-5 text-neutral-500">
            Verificadas en la configuración del pipeline del proyecto. Algunas etapas se
            activan según la vertical o la configuración de cada despliegue.
          </p>
        </div>

        <ol
          className="mx-auto mt-12 grid max-w-6xl grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
          role="list"
        >
          {stages.map((stage, index) => (
            <li
              key={stage.label}
              className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-center gap-3">
                <span
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-xs font-bold text-primary-700 ring-1 ring-primary-100"
                  aria-hidden="true"
                >
                  {index + 1}
                </span>
                <h3 className="text-sm font-semibold text-neutral-900">{stage.label}</h3>
              </div>
              <p className="mt-3 text-xs leading-5 text-neutral-500">{stage.description}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
