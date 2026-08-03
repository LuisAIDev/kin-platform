import type { EnterpriseDocument } from "../types/enterprise";

const FORMATS = ["PDF", "DOCX", "PPTX"] as const;
export type ExportFormat = (typeof FORMATS)[number];

interface ExportButtonsProps {
  /** Documentos de la versión (para habilitar botones por documento). */
  documents: EnterpriseDocument[];
  /** Callback de descarga de un documento en un formato. */
  onDownloadDocument: (type: string, format: ExportFormat) => void;
  /** Callback de descarga del bundle ZIP de un formato. */
  onDownloadBundle: (format: ExportFormat) => void;
  /** Indica si hay una descarga en curso. */
  busy?: boolean;
}

/** Botones de exportación de documentos Enterprise (PDF, DOCX, PPTX y ZIP). */
export function ExportButtons({
  documents,
  onDownloadDocument,
  onDownloadBundle,
  busy = false,
}: ExportButtonsProps) {
  return (
    <div data-testid="export-buttons">
      <div className="card-title">Exportar documentos</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginBottom: 8 }}>
        {FORMATS.map((format) => (
          <button
            key={format}
            type="button"
            className="btn"
            disabled={busy || documents.length === 0}
            onClick={() => onDownloadBundle(format)}
          >
            ZIP · {format}
          </button>
        ))}
      </div>
      {documents.map((document) => (
        <div key={document.id} className="doc-item">
          <span className="doc-name">{document.type.replace(/_/g, " ")}</span>
          <span style={{ display: "inline-flex", gap: 6 }}>
            {FORMATS.map((format) => (
              <button
                key={format}
                type="button"
                className="btn"
                disabled={busy}
                onClick={() => onDownloadDocument(document.type, format)}
              >
                {format}
              </button>
            ))}
          </span>
        </div>
      ))}
    </div>
  );
}
