import { ABOUT_CONTENT } from "./aboutContent";
import CategoryIcon from "./icons";
import SectionHeading from "./SectionHeading";

/**
 * Sección "Engineering Showcase": las áreas técnicas de KIN con evidencia real
 * del código. Las métricas de QA provienen de QA_METRICS (verificadas en la
 * última ejecución local de la suite).
 */
export default function ShowcaseSection() {
  const { showcase } = ABOUT_CONTENT;

  return (
    <section
      id="engineering-showcase"
      aria-labelledby="engineering-showcase-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={showcase.eyebrow}
          title={showcase.title}
          intro={showcase.intro}
        />

        <div className="mx-auto mt-14 grid max-w-6xl grid-cols-1 gap-6 lg:grid-cols-2">
          {showcase.groups.map((group) => (
            <article
              key={group.id}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8"
            >
              <div className="flex items-start gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-50 ring-1 ring-primary-100">
                  <CategoryIcon name={group.icon} className="h-6 w-6 text-primary-600" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-neutral-900">{group.title}</h3>
                  <p className="mt-0.5 text-sm leading-5 text-neutral-500">
                    {group.description}
                  </p>
                </div>
              </div>

              <ul className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2" role="list">
                {group.items.map((item) => (
                  <li
                    key={item.name}
                    className="rounded-lg border border-neutral-100 bg-neutral-50/60 px-4 py-3"
                  >
                    <p className="flex items-start gap-2 text-sm font-medium text-neutral-900">
                      <svg
                        className="mt-0.5 h-4 w-4 shrink-0 text-primary-500"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                        aria-hidden="true"
                      >
                        <path
                          fillRule="evenodd"
                          d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                          clipRule="evenodd"
                        />
                      </svg>
                      {item.name}
                    </p>
                    {item.description && (
                      <p className="mt-1 pl-6 text-xs leading-5 text-neutral-500">
                        {item.description}
                      </p>
                    )}
                  </li>
                ))}
              </ul>

              {group.metrics && (
                <div className="mt-6">
                  <div className="grid grid-cols-3 gap-3">
                    <div className="rounded-lg border border-primary-100 bg-primary-50/60 px-3 py-2.5 text-center">
                      <p className="text-xl font-bold text-primary-700">
                        {group.metrics.frontendTests}
                      </p>
                      <p className="mt-0.5 text-xs text-neutral-500">tests frontend</p>
                    </div>
                    <div className="rounded-lg border border-primary-100 bg-primary-50/60 px-3 py-2.5 text-center">
                      <p className="text-xl font-bold text-primary-700">{group.metrics.testFiles}</p>
                      <p className="mt-0.5 text-xs text-neutral-500">archivos de test</p>
                    </div>
                    <div className="rounded-lg border border-primary-100 bg-primary-50/60 px-3 py-2.5 text-center">
                      <p className="text-xl font-bold text-primary-700">{group.metrics.e2e}</p>
                      <p className="mt-0.5 text-xs text-neutral-500">E2E Sobre KIN</p>
                    </div>
                  </div>
                  <p className="mt-3 text-xs leading-5 text-neutral-400">
                    Métricas verificadas en la última ejecución local de la suite de
                    pruebas. Se actualizan con cada nueva ejecución.
                  </p>
                </div>
              )}
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
