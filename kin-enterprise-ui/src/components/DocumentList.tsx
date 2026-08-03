import type { EnterpriseDocument } from "../types/enterprise";

interface DocumentListProps {
  /** Documentos de la versión. */
  documents: EnterpriseDocument[];
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Listado de documentos de la versión Enterprise. */
export function DocumentList({ documents }: DocumentListProps) {
  if (documents.length === 0) {
    return <p className="hint">Aún no se han generado documentos.</p>;
  }
  return (
    <ul className="doc-list" data-testid="document-list">
      {documents.map((document) => (
        <li key={document.id} className="doc-item">
          <span className="doc-name">{document.type.replace(/_/g, " ")}</span>
          <span className="doc-meta">
            {formatBytes(document.size)} · {document.generatedBy}
          </span>
        </li>
      ))}
    </ul>
  );
}
