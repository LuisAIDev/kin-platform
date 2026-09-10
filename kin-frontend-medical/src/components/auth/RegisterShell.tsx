import Link from "next/link";

/**
 * Shell común para las páginas de registro: encabezado + formulario.
 * Reutilizable por Empresa y Salud.
 */
export default function RegisterShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <main className="flex-1 flex items-center justify-center px-6 py-10">
      <div className="w-full max-w-sm flex flex-col gap-4">
        <div className="text-center">
          <h1 className="text-2xl font-bold">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-neutral-500">{subtitle}</p>}
        </div>
        {children}
        <p className="text-center text-sm text-neutral-500">
          ¿Ya tienes cuenta?{" "}
          <Link href="/login" className="text-primary-600 underline hover:text-primary-700">
            Inicia sesión
          </Link>
        </p>
      </div>
    </main>
  );
}
