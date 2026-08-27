import { ARCH_ADR } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "Architecture Decision Records": documentación arquitectónica de
 * KIN. Se muestra el número verificado de ADRs y temas generales, sin publicar
 * el contenido de los documentos.
 */
export default function AdrSection() {
  return (
    <section
      id="adr"
      aria-labelledby="adr-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_ADR.eyebrow}
          title={ARCH_ADR.title}
          intro={ARCH_ADR.intro}
        />

        <div className="mx-auto mt-12 flex max-w-2xl flex-col items-center gap-2 rounded-2xl border border-primary-100 bg-white p-6 text-center shadow-sm">
          <p className="text-5xl font-extrabold tracking-tight text-primary-700">{ARCH_ADR.count}</p>
          <p className="text-base font-semibold text-neutral-900">ADRs documentados</p>
          <p className="mt-1 max-w-lg text-xs leading-5 text-neutral-500">
            Cada decisión relevante queda registrada con su contexto, alternativa y
            justificación. El contenido de los documentos es privado.
          </p>
        </div>

        <ul
          className="mx-auto mt-10 flex max-w-3xl flex-wrap items-center justify-center gap-2.5"
          role="list"
        >
          {ARCH_ADR.topics.map((topic) => (
            <li
              key={topic}
              className="rounded-full border border-primary-100 bg-white px-4 py-1.5 text-sm font-medium text-primary-700 shadow-sm"
            >
              {topic}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
