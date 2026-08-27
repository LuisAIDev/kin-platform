import { ARCH_CATEGORIES } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "19 categorías que guían el análisis": catálogo real de categorías de
 * proyecto, presentado de forma elegante. La categoría impulsa la selección
 * determinista de conocimiento y el análisis.
 */
export default function CategoriesSection() {
  return (
    <section
      id="categorias"
      aria-labelledby="categorias-title"
      className="relative overflow-hidden border-y border-primary-100 bg-gradient-to-br from-primary-50/60 via-white to-accent-50/20 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_CATEGORIES.eyebrow}
          title={ARCH_CATEGORIES.title}
          intro={ARCH_CATEGORIES.intro}
        />

        <div className="mx-auto mt-12 flex max-w-2xl flex-col items-center gap-2 rounded-2xl border border-primary-100 bg-white p-6 text-center shadow-sm">
          <p className="text-5xl font-extrabold tracking-tight text-primary-700">
            {ARCH_CATEGORIES.count}
          </p>
          <p className="text-base font-semibold text-neutral-900">categorías de proyecto</p>
          <p className="mt-1 max-w-lg text-xs leading-5 text-neutral-500">
            Verificadas en el catálogo de la base de datos. Cada categoría puede tener
            fuentes de conocimiento específicas, además de las fuentes generales.
          </p>
        </div>

        <ul
          className="mx-auto mt-12 grid max-w-4xl grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-3"
          role="list"
        >
          {ARCH_CATEGORIES.items.map((category) => (
            <li
              key={category}
              className="flex items-center gap-2.5 rounded-xl border border-neutral-200 bg-white px-4 py-3 shadow-sm"
            >
              <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary-500" aria-hidden="true" />
              <span className="text-sm font-medium text-neutral-700">{category}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
