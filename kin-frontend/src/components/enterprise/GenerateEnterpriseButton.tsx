"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { enterpriseApi } from "@/services/enterpriseApi";

interface GenerateEnterpriseButtonProps {
  /** Identificador del proyecto de KIN. */
  projectId: string;
}

/**
 * Botón "Generar Proyecto Empresarial" de la página del proyecto (M3G).
 *
 * <p>Solicita la generación (POST async) y, al ser aceptada (202) o si ya hay
 * una generación en curso (409), navega al Enterprise Dashboard para mostrar el
 * progreso SSE en vivo sin recargar la página. Ante un error del backend (p. ej.
 * 422 sin contexto) muestra el mensaje junto al botón.</p>
 */
export function GenerateEnterpriseButton({ projectId }: GenerateEnterpriseButtonProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async () => {
    if (loading) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const status = await enterpriseApi.generate(projectId, true);
      if (status === 202 || status === 409) {
        router.push(`/dashboard/projects/${projectId}/enterprise`);
        return;
      }
      setLoading(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setLoading(false);
    }
  };

  return (
    <div className="mt-3">
      <button
        type="button"
        onClick={() => void handleGenerate()}
        disabled={loading}
        className="block w-full rounded-xl bg-accent-600 px-4 py-2.5 text-sm font-medium text-white text-center hover:bg-accent-700 transition disabled:opacity-50"
      >
        {loading ? "Generando..." : "Generar Proyecto Empresarial"}
      </button>
      {error ? (
        <p className="text-xs text-red-600 mt-2" data-testid="generate-error">
          {error}
        </p>
      ) : null}
    </div>
  );
}
