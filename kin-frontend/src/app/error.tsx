"use client";

import { useEffect } from "react";

export default function GlobalError({ error, reset }: { error: Error; reset: () => void }) {
  useEffect(() => {
    console.error("[error-boundary]", error);
  }, [error]);

  return (
    <main role="alert" className="flex min-h-screen flex-col items-center justify-center bg-white px-6 text-center">
      <h1 className="text-2xl font-bold text-neutral-900">Algo salió mal</h1>
      <p className="mt-2 max-w-md text-sm text-neutral-500">
        Ocurrió un error inesperado. Puedes intentarlo de nuevo.
      </p>
      <button
        onClick={reset}
        className="mt-6 rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11"
      >
        Reintentar
      </button>
    </main>
  );
}
