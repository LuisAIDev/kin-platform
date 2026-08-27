import { ARCH_CAPABILITIES, type CapabilityCard } from "./architectureContent";
import CategoryIcon from "@/components/about/icons";
import SectionHeading from "@/components/about/SectionHeading";
/**
 * Sección "¿Qué demuestra KIN desde el punto de vista profesional?": tarjetas
 * de capacidades con evidencia real del proyecto. Sin frases exageradas: la
 * evidencia habla por sí misma.
 */
export default function CapabilitiesSection() {
  return (
    <section
      id="capacidades-profesionales"
      aria-labelledby="capacidades-profesionales-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_CAPABILITIES.eyebrow}
          title={ARCH_CAPABILITIES.title}
          intro={ARCH_CAPABILITIES.intro}
        />

        <div className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {ARCH_CAPABILITIES.cards.map((card: CapabilityCard) => (
            <article
              key={card.id}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-7"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-50 ring-1 ring-primary-100">
                <CategoryIcon name={card.icon} className="h-6 w-6 text-primary-600" />
              </div>
              <h3 className="mt-5 text-base font-semibold text-neutral-900">{card.title}</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">{card.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
