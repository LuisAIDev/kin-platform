import { ARCH_DIAGRAM } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

function LayerBox({
  name,
  description,
  badges,
}: {
  name: string;
  description: string;
  badges?: string[];
}) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white px-5 py-4 shadow-sm">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
        <p className="text-sm font-semibold text-neutral-900">{name}</p>
        {badges?.map((badge) => (
          <span
            key={badge}
            className="rounded-full border border-primary-100 bg-primary-50/70 px-2.5 py-0.5 text-[11px] font-medium text-primary-700"
          >
            {badge}
          </span>
        ))}
      </div>
      <p className="mt-1 text-xs leading-5 text-neutral-500">{description}</p>
    </div>
  );
}

function Connector() {
  return (
    <div className="flex justify-center py-1" aria-hidden="true">
      <svg
        className="h-4 w-4 text-primary-400"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
      >
        <path strokeLinecap="round" strokeLinejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
      </svg>
    </div>
  );
}

/**
 * Diagrama general de arquitectura: flujo conceptual de extremo a extremo,
 * comprensible sin conocer los frameworks internos. La política determinista
 * y el Knowledge Engine aparecen como capas propias. No se exponen nombres
 * internos de clases.
 */
export default function ArchDiagram() {
  const layers = ARCH_DIAGRAM.layers;
  const lastIndex = layers.length - 1;

  return (
    <section
      id="diagrama"
      aria-labelledby="diagrama-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow="Diagrama general"
          title={ARCH_DIAGRAM.title}
          intro="Del usuario a la respuesta: la política determinista decide, el Knowledge Engine adquiere conocimiento seguro y la IA comunica."
        />

        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-1 gap-8 lg:grid-cols-[1fr_220px]">
          <div className="flex flex-col" aria-label="Flujo de arquitectura del sistema">
            {layers.map((layer, index) => (
              <div key={layer.name}>
                <LayerBox name={layer.name} description={layer.description} badges={layer.badges} />
                {index !== lastIndex && <Connector />}
              </div>
            ))}
          </div>

          <aside
            className="lg:pt-6"
            aria-label="Componentes transversales"
          >
            <p className="text-xs font-semibold uppercase tracking-widest text-neutral-400">
              Transversales
            </p>
            <ul className="mt-4 space-y-3" role="list">
              {ARCH_DIAGRAM.transversal.map((item) => (
                <li
                  key={item}
                  className="rounded-xl border border-dashed border-primary-200 bg-primary-50/40 px-4 py-3 text-sm font-medium text-primary-800"
                >
                  {item}
                </li>
              ))}
            </ul>
          </aside>
        </div>
      </div>
    </section>
  );
}
