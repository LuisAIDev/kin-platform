import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-white px-6 text-center">
      <div className="text-6xl" aria-hidden="true">🔍</div>
      <h1 className="mt-4 text-2xl font-bold text-neutral-900">Página no encontrada</h1>
      <p className="mt-2 max-w-md text-sm text-neutral-500">
        La página que buscas no existe o fue movida.
      </p>
      <Link
        href="/"
        className="mt-6 rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition"
      >
        Volver al inicio
      </Link>
    </main>
  );
}
