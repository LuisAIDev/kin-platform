import { ARCH_OBSERVABILITY } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

/**
 * Sección "Observabilidad": cómo KIN mide y entiende su comportamiento en
 * producción. Nivel conceptual, sin configuraciones privadas.
 */
export default function ObservabilitySection() {
  return (
    <section
      id="observabilidad"
      aria-labelledby="observabilidad-title"
      className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_OBSERVABILITY.eyebrow}
          title={ARCH_OBSERVABILITY.title}
          intro={ARCH_OBSERVABILITY.intro}
        />

        <ul
          className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
          role="list"
        >
          {ARCH_OBSERVABILITY.items.map((item) => (
            <li
              key={item}
              className="flex items-start gap-3 rounded-xl border border-neutral-200 bg-white px-5 py-4 shadow-sm"
            >
              <svg
                className="mt-0.5 h-5 w-5 shrink-0 text-primary-600"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.5}
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z"
                />
              </svg>
              <span className="text-sm font-medium leading-6 text-neutral-700">{item}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
