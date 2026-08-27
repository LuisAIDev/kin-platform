import { ARCH_AI } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "IA aplicada con guardrails": KIN usa IA como componente de una
 * arquitectura mayor, no como sustituto de ella. Muestra la separación
 * determinista/evidencia/comunicación y los guardrails.
 */
export default function AiSection() {
  return (
    <section
      id="ia-aplicada"
      aria-labelledby="ia-aplicada-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_AI.eyebrow}
          title={ARCH_AI.title}
          intro={ARCH_AI.intro}
        />

        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {ARCH_AI.pillars.map((pillar) => (
            <article
              key={pillar.title}
              className="rounded-2xl border border-primary-100 bg-white p-6 shadow-sm"
            >
              <h3 className="text-base font-semibold text-neutral-900">{pillar.title}</h3>
              <p className="mt-2.5 text-sm leading-6 text-neutral-500">{pillar.description}</p>
            </article>
          ))}
        </div>

        <p className="mx-auto mt-12 text-center text-xs font-semibold uppercase tracking-widest text-neutral-400">
          Guardrails de la comunicación IA
        </p>
        <div className="mx-auto mt-6 grid max-w-6xl grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {ARCH_AI.items.map((item) => (
            <article
              key={item.label}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-7"
            >
              <h3 className="text-base font-semibold text-neutral-900">{item.label}</h3>
              <p className="mt-2.5 text-sm leading-6 text-neutral-500">{item.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
