"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
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

type MemoMetadata = {
  category: string | null;
  status: string | null;
  score: string | null;
  date: string | null;
};

const EMPTY_METADATA: MemoMetadata = {
  category: null,
  status: null,
  score: null,
  date: null,
};

const NO_DATA = "—";

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "Borrador",
  IN_PROGRESS: "En progreso",
  COMPLETED: "Completado",
  ARCHIVED: "Archivado",
  DELETED: "Eliminado",
};

const MONTHS = [
  "Enero",
  "Febrero",
  "Marzo",
  "Abril",
  "Mayo",
  "Junio",
  "Julio",
  "Agosto",
  "Septiembre",
  "Octubre",
  "Noviembre",
  "Diciembre",
];

const METADATA_LABELS: Record<string, keyof MemoMetadata> = {
  categoria: "category",
  "categoría": "category",
  estado: "status",
  fecha: "date",
  score: "score",
  "score de viabilidad": "score",
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
    <div className="memo-table-wrapper my-5 overflow-x-auto rounded-lg border border-neutral-200">
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
  .memo-print-portal {
    display: none;
  }

  @media print {
    @page {
      size: A4 portrait;
      margin: 18mm 16mm 20mm;
    }

    html,
    body {
      margin: 0 !important;
      padding: 0 !important;
      height: auto !important;
      overflow: visible !important;
      background: #ffffff !important;
      color: #111111 !important;
    }

    body > *:not(.memo-print-portal) {
      display: none !important;
    }

    .memo-print-portal {
      display: block !important;
      background: #ffffff !important;
      color: #111111 !important;
      font-family: Georgia, "Times New Roman", serif;
      font-size: 11.5pt;
      line-height: 1.65;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }

    .memo-print-portal *,
    .memo-print-portal *::before,
    .memo-print-portal *::after {
      box-shadow: none !important;
      text-shadow: none !important;
    }

    .memo-print-portal button,
    .memo-print-portal [role="button"],
    .memo-print-portal nav,
    .memo-print-portal .memo-no-print {
      display: none !important;
    }

    .memo-cover {
      display: flex !important;
      flex-direction: column;
      justify-content: space-between;
      min-height: 236mm;
      padding: 4mm 0;
      break-after: page;
      page-break-after: always;
      break-inside: avoid;
      page-break-inside: avoid;
    }

    .memo-cover-brand {
      display: flex;
      align-items: center;
      gap: 4mm;
    }

    .memo-cover-mark {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 14mm;
      height: 14mm;
      border-radius: 3mm;
      background: #111111 !important;
      color: #ffffff !important;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 20pt;
      font-weight: 700;
      line-height: 1;
    }

    .memo-cover-wordmark {
      font-family: Arial, Helvetica, sans-serif;
      font-size: 19pt;
      font-weight: 700;
      letter-spacing: 0.16em;
      color: #111111 !important;
    }

    .memo-cover-body {
      max-width: 160mm;
      margin: auto 0;
    }

    .memo-cover-eyebrow {
      margin: 0 0 12mm;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 8.5pt;
      font-weight: 700;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      color: #555555 !important;
    }

    .memo-cover-title {
      margin: 0;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 32pt;
      font-weight: 700;
      line-height: 1.12;
      letter-spacing: -0.02em;
      color: #111111 !important;
      overflow-wrap: anywhere;
    }

    .memo-cover-date {
      margin: 11mm 0 0;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 12pt;
      color: #333333 !important;
    }

    .memo-cover-subtitle {
      margin: 6mm 0 0;
      padding-top: 5mm;
      border-top: 1px solid #111111;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 10pt;
      font-weight: 700;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      color: #111111 !important;
    }

    .memo-cover-footer {
      display: flex;
      justify-content: space-between;
      gap: 10mm;
      padding-top: 5mm;
      border-top: 1px solid #d4d4d4;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 8pt;
      letter-spacing: 0.04em;
      color: #666666 !important;
    }

    .memo-print-content {
      max-width: none !important;
      margin: 0 !important;
    }

    .memo-print-header {
      margin: 0 0 8mm;
    }

    .memo-print-kicker {
      margin: 0 0 3mm;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 8.5pt;
      font-weight: 700;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: #666666 !important;
    }

    .memo-print-title {
      margin: 0 0 6mm;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 22pt;
      font-weight: 700;
      line-height: 1.15;
      color: #111111 !important;
      overflow-wrap: anywhere;
    }

    .memo-print-content h1,
    .memo-print-content h2,
    .memo-print-content h3,
    .memo-print-content h4,
    .memo-print-content h5,
    .memo-print-content h6 {
      font-family: Arial, Helvetica, sans-serif;
      color: #111111 !important;
      line-height: 1.25;
    }

    .memo-print-content h1 {
      margin: 0 0 6mm;
      font-size: 22pt;
    }

    .memo-print-content h2 {
      margin: 8mm 0 3mm;
      padding-bottom: 2mm;
      border-bottom: 1px solid #d4d4d4;
      font-size: 15pt;
    }

    .memo-print-content h3 {
      margin: 6mm 0 2mm;
      font-size: 12pt;
    }

    .memo-print-content h4,
    .memo-print-content h5,
    .memo-print-content h6 {
      margin: 5mm 0 2mm;
      font-size: 10.5pt;
    }

    .memo-print-content p {
      margin: 0 0 3mm;
      color: #222222 !important;
      orphans: 3;
      widows: 3;
    }

    .memo-print-content ul,
    .memo-print-content ol {
      margin: 0 0 4mm;
      padding-left: 6mm;
    }

    .memo-print-content li {
      margin-bottom: 1.5mm;
      color: #222222 !important;
    }

    .memo-print-content strong {
      color: #111111 !important;
    }

    .memo-print-content a {
      color: #111111 !important;
      text-decoration: underline;
    }

    .memo-print-content blockquote {
      margin: 4mm 0;
      padding: 2mm 4mm;
      border-left: 3px solid #9a9a9a !important;
      background: #fafafa !important;
      color: #333333 !important;
      font-style: italic;
    }

    .memo-print-content table {
      width: 100%;
      margin: 4mm 0;
      border-collapse: collapse;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 9pt;
    }

    .memo-print-content .memo-table-wrapper {
      margin: 0 !important;
      border: 0 !important;
      border-radius: 0 !important;
      overflow: visible !important;
    }

    .memo-print-content th,
    .memo-print-content td {
      padding: 2mm 2.5mm;
      border: 1px solid #c9c9c9 !important;
      text-align: left;
      vertical-align: top;
      color: #222222 !important;
    }

    .memo-print-content th {
      background: #f2f2f2 !important;
      color: #111111 !important;
    }

    .memo-print-meta {
      margin: 0 0 8mm !important;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 9pt;
    }

    .memo-print-meta th {
      width: 25%;
      background: #f2f2f2 !important;
      font-size: 7.5pt;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: #555555 !important;
    }

    .memo-print-meta td {
      font-size: 10pt;
      font-weight: 700;
      color: #111111 !important;
    }

    .memo-print-content code {
      padding: 0.5mm 1.5mm;
      border-radius: 1mm;
      background: #f5f5f5 !important;
      color: #111111 !important;
      font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
      font-size: 9pt;
    }

    .memo-print-content pre {
      margin: 4mm 0;
      padding: 3mm;
      border: 1px solid #e0e0e0 !important;
      border-radius: 2mm;
      background: #f7f7f7 !important;
      color: #111111 !important;
      font-size: 8.5pt;
      line-height: 1.5;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }

    .memo-print-content pre code {
      padding: 0;
      border: 0 !important;
      background: transparent !important;
      color: inherit !important;
    }

    .memo-print-content hr {
      margin: 6mm 0;
      border: 0;
      border-top: 1px solid #d4d4d4;
    }

    .memo-print-content h1,
    .memo-print-content h2,
    .memo-print-content h3,
    .memo-print-content h4,
    .memo-print-content h5,
    .memo-print-content h6,
    .memo-print-content p,
    .memo-print-content ul,
    .memo-print-content ol,
    .memo-print-content li,
    .memo-print-content blockquote,
    .memo-print-content pre,
    .memo-print-content table,
    .memo-print-content tr,
    .memo-print-meta,
    .memo-print-meta tr {
      break-inside: avoid;
      page-break-inside: avoid;
    }

    .memo-print-content h1,
    .memo-print-content h2,
    .memo-print-content h3,
    .memo-print-content h4,
    .memo-print-content h5,
    .memo-print-content h6 {
      break-after: avoid;
      page-break-after: avoid;
    }
  }
`;

function cleanValue(value: string | null | undefined): string | null {
  if (!value) return null;
  const trimmed = value.trim();
  if (!trimmed) return null;
  return /^(n\/d|n\/a|not_available|no disponible|—|-)$/i.test(trimmed)
    ? null
    : trimmed;
}

function displayValue(value: string | null): string {
  return cleanValue(value) ?? NO_DATA;
}

function displayStatus(value: string | null): string {
  const label = cleanValue(value);
  if (!label) return NO_DATA;
  return (
    STATUS_LABELS[label.toUpperCase()] ??
    label
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase())
  );
}

function displayScore(value: string | null): string {
  const score = cleanValue(value);
  if (!score) return NO_DATA;
  return /^\d+(?:[.,]\d+)?$/.test(score) ? `${score} / 100` : score;
}

function formatExecutiveDate(value?: string | null): string {
  const parsed = value ? new Date(value) : new Date();
  const date = Number.isNaN(parsed.getTime()) ? new Date() : parsed;
  const day = date.getUTCDate();
  const month = MONTHS[date.getUTCMonth()] ?? "";
  const year = date.getUTCFullYear();

  return `${day} de ${month} de ${year}`;
}

function extractMetadataLine(line: string, metadata: MemoMetadata): void {
  const pairs = line.matchAll(/\*\*([^*]+?)\*\*\s*:?\s*([^|*]+)/g);
  for (const match of pairs) {
    const rawLabel = match[1]?.replace(/:\s*$/, "").trim().toLowerCase();
    const key = rawLabel ? METADATA_LABELS[rawLabel] : undefined;
    if (!key) continue;
    const value = match[2]?.replace(/[|*]/g, "").trim();
    if (value) metadata[key] = value;
  }
}

function parseMemo(markdown: string): {
  title: string | null;
  metadata: MemoMetadata;
  body: string;
} {
  const metadata: MemoMetadata = { ...EMPTY_METADATA };
  const bodyLines: string[] = [];
  let title: string | null = null;
  let headerOpen = true;

  for (const line of markdown.replace(/\r\n/g, "\n").split("\n")) {
    const trimmed = line.trim();

    if (headerOpen && !title) {
      const heading = trimmed.match(/^#\s+(.+)$/);
      if (heading) {
        title = heading[1].trim();
        continue;
      }
    }

    if (headerOpen && trimmed === "") {
      continue;
    }

    if (
      headerOpen &&
      /\*\*(?:Categor[ií]a|Estado|Fecha|Score(?:\s+de\s+Viabilidad)?)\s*:?\*\*/i.test(
        trimmed
      )
    ) {
      extractMetadataLine(trimmed, metadata);
      continue;
    }

    headerOpen = false;
    bodyLines.push(line);
  }

  while (bodyLines.length > 0 && bodyLines[0].trim() === "") bodyLines.shift();
  while (
    bodyLines.length > 0 &&
    bodyLines[bodyLines.length - 1].trim() === ""
  ) {
    bodyLines.pop();
  }

  return { title, metadata, body: bodyLines.join("\n") };
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

function MemoMetadataTable({
  metadata,
  date,
  className = "",
}: {
  metadata: MemoMetadata;
  date: string;
  className?: string;
}) {
  const cells: Array<[string, string]> = [
    ["Categoría", displayValue(metadata.category)],
    ["Estado", displayStatus(metadata.status)],
    ["Score", displayScore(metadata.score)],
    ["Fecha", date],
  ];

  return (
    <table
      className={`memo-meta-table w-full table-fixed border-collapse text-left ${className}`}
    >
      <thead>
        <tr>
          {cells.map(([label]) => (
            <th
              key={label}
              scope="col"
              className="border border-neutral-200 bg-neutral-50 px-3 py-2 text-[10px] font-semibold uppercase tracking-[0.12em] text-neutral-500"
            >
              {label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        <tr>
          {cells.map(([label, value]) => (
            <td
              key={label}
              className="border border-neutral-200 px-3 py-2.5 text-sm font-semibold text-neutral-900"
            >
              {value}
            </td>
          ))}
        </tr>
      </tbody>
    </table>
  );
}

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

  const memoDoc = useMemo(() => parseMemo(markdown ?? ""), [markdown]);
  const documentTitle = memoDoc.title || projectTitle;
  const documentDate = useMemo(
    () => formatExecutiveDate(memoDoc.metadata.date),
    [memoDoc.metadata.date]
  );

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
    if (!markdown) return;
    window.print();
  }, [markdown]);

  const actionsDisabled = loading || !markdown;

  const renderedMemo = markdown ? (
    <article id="investment-memo-content" className="mx-auto max-w-3xl">
      <header className="mb-6">
        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-primary-600">
          KIN · Investment Memo
        </p>
        <h1 className="mt-2 text-2xl font-bold tracking-tight text-neutral-900">
          {documentTitle}
        </h1>
        <div className="mt-5 overflow-hidden rounded-xl border border-neutral-200">
          <MemoMetadataTable metadata={memoDoc.metadata} date={documentDate} />
        </div>
      </header>
      {memoDoc.body ? (
        <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
          {memoDoc.body}
        </ReactMarkdown>
      ) : null}
    </article>
  ) : null;

  const printView =
    typeof document !== "undefined" && markdown
      ? createPortal(
          <div className="memo-print-portal" aria-hidden="true">
            <style>{PRINT_STYLES}</style>
            <section className="memo-cover">
              <header className="memo-cover-brand">
                <span className="memo-cover-mark">K</span>
                <span className="memo-cover-wordmark">KIN</span>
              </header>
              <div className="memo-cover-body">
                <p className="memo-cover-eyebrow">Documento de inversión</p>
                <h1 className="memo-cover-title">{documentTitle}</h1>
                <p className="memo-cover-date">{documentDate}</p>
                <p className="memo-cover-subtitle">
                  Investment Memo — Confidencial
                </p>
              </div>
              <footer className="memo-cover-footer">
                <span>KIN Platform · Knowledge, Innovation &amp; Navigation</span>
                <span>Documento confidencial para uso exclusivo de inversores.</span>
              </footer>
            </section>
            <article className="memo-print-content">
              <header className="memo-print-header">
                <p className="memo-print-kicker">KIN · Investment Memo</p>
                <h1 className="memo-print-title">{documentTitle}</h1>
              </header>
              <MemoMetadataTable
                metadata={memoDoc.metadata}
                date={documentDate}
                className="memo-print-meta"
              />
              {memoDoc.body ? (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={markdownComponents}
                >
                  {memoDoc.body}
                </ReactMarkdown>
              ) : null}
            </article>
          </div>,
          document.body
        )
      : null;

  return (
    <>
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
              Imprimir
            </button>
            <button
              type="button"
              onClick={handlePrint}
              disabled={actionsDisabled}
              aria-label="Guardar documento de inversión en PDF"
              className={toolbarButtonClass}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              Guardar en PDF
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
      {printView}
    </>
  );
}
