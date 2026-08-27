import { ARCH_SUMMARY } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "KIN en una mirada técnica": resumen ejecutivo de 30 segundos para
 * evaluadores técnicos (problema, arquitectura, IA, control).
 */
export default function SummarySection() {
  return (
    <section
      id="resumen-ejecutivo"
      aria-labelledby="resumen-ejecutivo-title"
      className="border-y border-neutral-100 bg-white py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_SUMMARY.eyebrow}
          title={ARCH_SUMMARY.title}
          intro={ARCH_SUMMARY.intro}
        />

        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-1 gap-6 sm:grid-cols-2">
          {ARCH_SUMMARY.items.map((item, index) => (
            <article
              key={item.title}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8"
            >
              <div className="flex items-center gap-2.5">
                <span
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-xs font-bold text-primary-700 ring-1 ring-primary-100"
                  aria-hidden="true"
                >
                  {index + 1}
                </span>
                <h3 className="text-base font-semibold text-neutral-900">{item.title}</h3>
              </div>
              <p className="mt-3 text-sm leading-6 text-neutral-500">{item.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
