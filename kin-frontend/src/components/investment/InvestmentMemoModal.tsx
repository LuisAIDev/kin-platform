"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { downloadBlob } from "@/services/enterpriseApi";
import Skeleton from "@/components/ui/Skeleton";

type Props = {
  projectTitle: string;
  markdown: string | null;
  loading: boolean;
  error: string | null;
  onClose: () => void;
  onRetry: () => void;
};

const markdownComponents: Components = {
  h1: ({ children }) => (
    <h1 className="mb-4 text-2xl font-bold tracking-tight text-neutral-900">
      {children}
    </h1>
  ),
  h2: ({ children }) => (
    <h2 className="mt-8 mb-3 border-b border-neutral-200 pb-2 text-lg font-semibold text-neutral-900">
      {children}
    </h2>
  ),
  h3: ({ children }) => (
    <h3 className="mt-6 mb-2 text-base font-semibold text-neutral-900">
      {children}
    </h3>
  ),
  p: ({ children }) => (
    <p className="mb-3 text-sm leading-7 text-neutral-700">{children}</p>
  ),
  strong: ({ children }) => (
    <strong className="font-semibold text-neutral-900">{children}</strong>
  ),
  ul: ({ children }) => (
    <ul className="mb-4 list-disc space-y-1 pl-5 text-sm leading-7 text-neutral-700">
      {children}
    </ul>
  ),
  ol: ({ children }) => (
    <ol className="mb-4 list-decimal space-y-1 pl-5 text-sm leading-7 text-neutral-700">
      {children}
    </ol>
  ),
  li: ({ children }) => <li className="pl-1">{children}</li>,
  blockquote: ({ children }) => (
    <blockquote className="my-4 border-l-4 border-primary-200 bg-primary-50/50 px-4 py-2 text-sm italic text-neutral-700">
      {children}
    </blockquote>
  ),
  a: ({ href, children }) => (
    <a
      href={href}
      target="_blank"
      rel="noreferrer noopener"
      className="font-medium text-primary-700 underline underline-offset-2 hover:text-primary-800"
    >
      {children}
    </a>
  ),
  table: ({ children }) => (
    <div className="my-5 overflow-x-auto rounded-lg border border-neutral-200">
      <table className="w-full border-collapse text-left text-sm">{children}</table>
    </div>
  ),
  thead: ({ children }) => (
    <thead className="bg-neutral-50 text-neutral-900">{children}</thead>
  ),
  th: ({ children }) => (
    <th className="border-b border-neutral-200 px-3 py-2 font-semibold">{children}</th>
  ),
  td: ({ children }) => (
    <td className="border-b border-neutral-100 px-3 py-2 align-top text-neutral-700">
      {children}
    </td>
  ),
  code: ({ children }) => (
    <code className="rounded bg-neutral-100 px-1.5 py-0.5 font-mono text-xs text-neutral-800">
      {children}
    </code>
  ),
  pre: ({ children }) => (
    <pre className="my-4 overflow-x-auto rounded-lg bg-neutral-900 p-4 text-xs leading-6 text-neutral-100 [&>code]:bg-transparent [&>code]:p-0 [&>code]:text-xs [&>code]:text-inherit">
      {children}
    </pre>
  ),
  hr: () => <hr className="my-6 border-neutral-200" />,
};

const PRINT_STYLES = `
  @page { margin: 18mm; }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    color: #171717;
    font-family: Georgia, "Times New Roman", serif;
    font-size: 11.5pt;
    line-height: 1.6;
  }
  article { max-width: 100%; }
  h1, h2, h3, h4, h5, h6 {
    color: #0a0a0a;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
    line-height: 1.25;
  }
  h1 { margin: 0 0 18px; font-size: 24pt; }
  h2 { margin: 26px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #d4d4d4; font-size: 16pt; }
  h3 { margin: 20px 0 8px; font-size: 13pt; }
  h4 { margin: 16px 0 6px; font-size: 11.5pt; }
  p { margin: 0 0 10px; }
  ul, ol { margin: 0 0 12px; padding-left: 22px; }
  li { margin-bottom: 4px; }
  strong { color: #0a0a0a; }
  a { color: #1d4ed8; text-decoration: underline; }
  blockquote {
    margin: 14px 0;
    padding: 8px 14px;
    border-left: 4px solid #bfdbfe;
    background: #eff6ff;
    color: #404040;
    font-style: italic;
  }
  table { width: 100%; margin: 14px 0; border-collapse: collapse; font-size: 10pt; }
  th, td { padding: 6px 8px; border: 1px solid #d4d4d4; text-align: left; vertical-align: top; }
  th { background: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; }
  code {
    padding: 1px 4px;
    border-radius: 4px;
    background: #f5f5f5;
    font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
    font-size: 9.5pt;
  }
  pre { padding: 10px 12px; border-radius: 6px; background: #f7f7f7; color: #171717; border: 1px solid #e5e5e5; overflow-wrap: anywhere; white-space: pre-wrap; }
  pre code { padding: 0; background: transparent; color: inherit; }
  hr { margin: 20px 0; border: 0; border-top: 1px solid #d4d4d4; }
`;

const HTML_ESCAPES: Record<string, string> = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
};

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => HTML_ESCAPES[char]);
}

function slugify(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60);
}

const toolbarButtonClass =
  "inline-flex min-h-10 items-center justify-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-xs font-medium text-neutral-700 transition hover:bg-neutral-100 disabled:cursor-not-allowed disabled:opacity-50";

function MemoSkeleton() {
  return (
    <div className="mx-auto max-w-3xl space-y-4" role="status" aria-live="polite">
      <div className="flex items-center gap-3 rounded-xl border border-primary-100 bg-primary-50 px-4 py-3 text-sm text-primary-800">
        <span
          className="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-primary-300 border-t-primary-700"
          aria-hidden="true"
        />
        Generando el documento de inversión. Esto puede tardar unos segundos.
      </div>
      <Skeleton className="h-8 w-2/3" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-11/12" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-4/5" />
      <Skeleton className="mt-6 h-6 w-1/2" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-10/12" />
      <Skeleton className="h-4 w-full" />
    </div>
  );
}

function MemoError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div
      className="mx-auto flex max-w-xl flex-col items-center rounded-2xl border border-red-100 bg-red-50 px-6 py-10 text-center"
      role="alert"
    >
      <div
        className="flex h-11 w-11 items-center justify-center rounded-full bg-red-100 text-lg font-bold text-red-600"
        aria-hidden="true"
      >
        !
      </div>
      <p className="mt-4 text-sm font-semibold text-neutral-900">
        No se pudo generar el documento
      </p>
      <p className="mt-1 text-sm text-neutral-600">{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-5 inline-flex min-h-11 items-center justify-center rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-neutral-800"
      >
        Reintentar
      </button>
    </div>
  );
}

export default function InvestmentMemoModal({
  projectTitle,
  markdown,
  loading,
  error,
  onClose,
  onRetry,
}: Props) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    closeButtonRef.current?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(false), 2000);
    return () => window.clearTimeout(timer);
  }, [copied]);

  const handleCopy = useCallback(async () => {
    if (!markdown) return;
    try {
      await navigator.clipboard.writeText(markdown);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }, [markdown]);

  const handleDownload = useCallback(() => {
    if (!markdown) return;
    const filename = `investment-memo-${slugify(projectTitle) || "proyecto"}.md`;
    downloadBlob(
      new Blob([markdown], { type: "text/markdown;charset=utf-8" }),
      filename
    );
  }, [markdown, projectTitle]);

  const handlePrint = useCallback(() => {
    const content = document.getElementById("investment-memo-content");
    if (!content) return;

    const printWindow = window.open("", "_blank");
    if (!printWindow) return;

    printWindow.opener = null;
    printWindow.document.write(
      `<!doctype html><html lang="es"><head><meta charset="utf-8" />` +
        `<title>${escapeHtml(projectTitle)}</title>` +
        `<style>${PRINT_STYLES}</style></head><body>` +
        `<article>${content.innerHTML}</article></body></html>`
    );
    printWindow.document.close();
    printWindow.focus();
    printWindow.setTimeout(() => printWindow.print(), 200);
  }, [projectTitle]);

  const actionsDisabled = loading || !markdown;

  const renderedMemo = useMemo(
    () =>
      markdown ? (
        <article id="investment-memo-content" className="mx-auto max-w-3xl">
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={markdownComponents}
          >
            {markdown}
          </ReactMarkdown>
        </article>
      ) : null,
    [markdown]
  );

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-3 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="investment-memo-title"
      onClick={onClose}
    >
      <div
        className="flex max-h-[92vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex items-start justify-between gap-4 border-b border-neutral-200 px-5 py-4 sm:px-6">
          <div className="min-w-0">
            <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-primary-600">
              KIN · Documento de inversión
            </p>
            <h2
              id="investment-memo-title"
              className="mt-1 truncate text-lg font-bold tracking-tight text-neutral-900 sm:text-xl"
            >
              {projectTitle}
            </h2>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            aria-label="Cerrar documento de inversión"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-neutral-500 transition hover:bg-neutral-100 hover:text-neutral-900"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div className="flex flex-wrap items-center gap-2 border-b border-neutral-200 bg-neutral-50 px-5 py-3 sm:px-6">
          <button
            type="button"
            onClick={() => void handleCopy()}
            disabled={actionsDisabled}
            className={toolbarButtonClass}
          >
            {copied ? "Copiado" : "Copiar"}
          </button>
          <button
            type="button"
            onClick={handlePrint}
            disabled={actionsDisabled}
            className={toolbarButtonClass}
          >
            Imprimir / PDF
          </button>
          <button
            type="button"
            onClick={handleDownload}
            disabled={actionsDisabled}
            className={toolbarButtonClass}
          >
            Descargar .md
          </button>
          <button
            type="button"
            onClick={onRetry}
            disabled={loading}
            className={`${toolbarButtonClass} border-primary-200 text-primary-700 hover:bg-primary-50 sm:ml-auto`}
          >
            {loading ? "Generando..." : "Regenerar"}
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-6 sm:px-8 sm:py-8">
          {loading ? <MemoSkeleton /> : null}
          {!loading && error ? <MemoError message={error} onRetry={onRetry} /> : null}
          {!loading && !error ? renderedMemo : null}
        </div>
      </div>
    </div>
  );
}
