import {
  ABOUT_CONTENT,
  type ArchitectureHub,
  type ArchitectureHubNode,
  type ArchitectureLayer,
} from "./aboutContent";
import SectionHeading from "./SectionHeading";

function NodeBox({ node, highlight }: { node: ArchitectureHubNode; highlight?: boolean }) {
  return (
    <div
      className={
        highlight
          ? "rounded-xl border border-primary-200 bg-primary-50/60 px-4 py-3 text-center"
          : "rounded-xl border border-neutral-200 bg-white px-4 py-3 text-center shadow-sm"
      }
    >
      <p className="text-sm font-semibold text-neutral-900">{node.label}</p>
      <p className="mt-0.5 text-xs leading-5 text-neutral-500">{node.description}</p>
    </div>
  );
}

function Connector() {
  return (
    <div className="flex justify-center py-1.5" aria-hidden="true">
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

function HubDiagram({ hub }: { hub: ArchitectureHub }) {
  return (
    <div className="mx-auto max-w-3xl" aria-label="Diagrama de arquitectura">
      <NodeBox node={hub.root} highlight />
      <Connector />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {hub.branches.map((branch) => (
          <NodeBox key={branch.label} node={branch} />
        ))}
      </div>
      <Connector />
      <NodeBox node={hub.base} highlight />
      <Connector />
      <NodeBox node={hub.deploy} />
    </div>
  );
}

function LayerFlow({ title, layers }: { title: string; layers: ArchitectureLayer[] }) {
  return (
    <div className="flex-1">
      <h3 className="text-center text-base font-semibold text-neutral-900">{title}</h3>
      <ol className="mx-auto mt-6 flex max-w-md flex-col">
        {layers.map((layer, index) => {
          const isLast = index === layers.length - 1;
          return (
            <li key={layer.label}>
              <div className="rounded-xl border border-neutral-200 bg-white px-4 py-3 shadow-sm">
                <p className="text-sm font-semibold text-neutral-900">{layer.label}</p>
                <p className="mt-0.5 text-xs leading-5 text-neutral-500">{layer.description}</p>
              </div>
              {!isLast && <Connector />}
            </li>
          );
        })}
      </ol>
    </div>
  );
}

/**
 * Sección "Arquitectura de KIN": diagrama de visión general, componentes del
 * sistema y flujos detallados (sistema e integración de IA). Solo representa
 * capas que existen en el código real de KIN.
 */
export default function ArchitectureSection() {
  const { architecture } = ABOUT_CONTENT;

  return (
    <section
      id="arquitectura"
      aria-labelledby="arquitectura-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={architecture.eyebrow}
          title={architecture.title}
          intro={architecture.intro}
        />

        <HubDiagram hub={architecture.hub} />

        <div className="mx-auto mt-14 max-w-5xl">
          <h3 className="text-center text-base font-semibold text-neutral-900">
            Componentes del sistema
          </h3>
          <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {architecture.components.map((component) => (
              <div
                key={component.label}
                className="rounded-lg border border-neutral-200 bg-white px-4 py-3 shadow-sm"
              >
                <p className="text-sm font-semibold text-neutral-900">{component.label}</p>
                <p className="mt-0.5 text-xs leading-5 text-neutral-500">
                  {component.description}
                </p>
              </div>
            ))}
          </div>
        </div>

        <div className="mx-auto mt-14 flex max-w-5xl flex-col gap-10 lg:flex-row lg:items-start lg:justify-center lg:gap-16">
          {architecture.flows.map((flow) => (
            <LayerFlow key={flow.id} title={flow.title} layers={flow.layers} />
          ))}
        </div>
      </div>
    </section>
  );
}
