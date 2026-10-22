"use client";

import dynamic from "next/dynamic";
import { useCallback, useState } from "react";
import { investmentMemoService } from "@/services/investmentMemo";

const InvestmentMemoModal = dynamic(() => import("./InvestmentMemoModal"), {
  ssr: false,
  loading: () => (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="status"
      aria-live="polite"
    >
      <div className="flex items-center gap-3 rounded-xl bg-white px-5 py-4 text-sm font-medium text-neutral-700 shadow-xl">
        <span
          className="h-4 w-4 animate-spin rounded-full border-2 border-neutral-300 border-t-neutral-900"
          aria-hidden="true"
        />
        Abriendo documento...
      </div>
    </div>
  ),
});

type Props = {
  projectId: string;
  projectTitle: string;
};

function errorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "No se pudo generar el documento de inversión.";
}

export function InvestmentMemoButton({ projectId, projectTitle }: Props) {
  const [open, setOpen] = useState(false);
  const [markdown, setMarkdown] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadMemo = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const memo = await investmentMemoService.get(projectId);
      setMarkdown(memo);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  const handleOpen = useCallback(() => {
    setOpen(true);
    if (!markdown && !loading) {
      void loadMemo();
    }
  }, [loading, loadMemo, markdown]);

  const handleClose = useCallback(() => setOpen(false), []);

  const handleRetry = useCallback(() => {
    if (loading) return;
    setMarkdown(null);
    void loadMemo();
  }, [loadMemo, loading]);

  return (
    <>
      <button
        type="button"
        onClick={handleOpen}
        aria-haspopup="dialog"
        className="mt-3 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-neutral-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-800"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
          <polyline points="10 9 9 9 8 9" />
        </svg>
        Ver Documento de Inversión
      </button>

      {open ? (
        <InvestmentMemoModal
          projectTitle={projectTitle}
          markdown={markdown}
          loading={loading}
          error={error}
          onClose={handleClose}
          onRetry={handleRetry}
        />
      ) : null}
    </>
  );
}
