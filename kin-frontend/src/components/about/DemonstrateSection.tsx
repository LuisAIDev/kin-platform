import { ABOUT_CONTENT } from "./aboutContent";
import CategoryIcon from "./icons";
import SectionHeading from "./SectionHeading";

/**
 * Sección "Lo que KIN demuestra": capacidades de ingeniería respaldadas por el
 * código real del proyecto, en tarjetas limpias.
 */
export default function DemonstrateSection() {
  const { demonstrates } = ABOUT_CONTENT;

  return (
    <section
      id="lo-que-kin-demuestra"
      aria-labelledby="lo-que-kin-demuestra-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={demonstrates.eyebrow}
          title={demonstrates.title}
          intro={demonstrates.intro}
        />

        <div className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {demonstrates.cards.map((card) => (
            <div
              key={card.id}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-7"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-50 ring-1 ring-primary-100">
                <CategoryIcon name={card.icon} className="h-6 w-6 text-primary-600" />
              </div>
              <h3 className="mt-5 text-base font-semibold text-neutral-900">{card.title}</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">{card.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
