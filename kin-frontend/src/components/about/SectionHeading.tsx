interface Props {
  eyebrow?: string;
  title: string;
  intro?: string;
}

/** Encabezado de sección reutilizado por las secciones del módulo "Sobre KIN". */
export default function SectionHeading({ eyebrow, title, intro }: Props) {
  return (
    <div className="mx-auto max-w-2xl text-center">
      {eyebrow && (
        <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-3">
          {eyebrow}
        </p>
      )}
      <h2 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">{title}</h2>
      {intro && <p className="mt-4 text-lg leading-7 text-neutral-500">{intro}</p>}
    </div>
  );
}
