import { ARCH_CLOUD } from "./architectureContent";
import SectionHeading from "@/components/about/SectionHeading";

function FlowConnector() {
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
 * Sección "Cloud Architecture": infraestructura de despliegue a nivel de
 * conceptos y proveedores. Sin secretos ni configuración privada.
 */
export default function CloudSection() {
  const { flow } = ARCH_CLOUD;
  const lastIndex = flow.length - 1;

  return (
    <section
      id="cloud"
      aria-labelledby="cloud-title"
      className="py-20 sm:py-24"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={ARCH_CLOUD.eyebrow}
          title={ARCH_CLOUD.title}
          intro={ARCH_CLOUD.intro}
        />

        <ol className="mx-auto mt-14 max-w-2xl" role="list">
          {flow.map((step, index) => (
            <li key={step.label}>
              <div className="flex items-start gap-4 rounded-xl border border-neutral-200 bg-white px-5 py-4 shadow-sm">
                <span
                  className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-sm font-bold text-primary-700 ring-1 ring-primary-100"
                  aria-hidden="true"
                >
                  {index + 1}
                </span>
                <div>
                  <p className="text-sm font-semibold text-neutral-900">{step.label}</p>
                  <p className="mt-0.5 text-xs leading-5 text-neutral-500">{step.description}</p>
                </div>
              </div>
              {index !== lastIndex && <FlowConnector />}
            </li>
          ))}
        </ol>

        <p className="mx-auto mt-8 max-w-2xl text-center text-xs leading-5 text-neutral-400">
          Nivel de infraestructura: la configuración exacta del despliegue y las
          credenciales asociadas no se publican.
        </p>
      </div>
    </section>
  );
}
