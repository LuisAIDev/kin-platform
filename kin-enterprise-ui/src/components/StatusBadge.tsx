const VALID_STATUSES = new Set([
  "REQUESTED",
  "RUNNING",
  "DOCUMENT_GENERATED",
  "COMPLETED",
  "FAILED",
]);

interface StatusBadgeProps {
  /** Estado de la generación. */
  status: string;
}

/** Insignia de estado de la generación Enterprise. */
export function StatusBadge({ status }: StatusBadgeProps) {
  const className = VALID_STATUSES.has(status) ? `badge-${status}` : "badge-unknown";
  return (
    <span data-testid="status-badge" className={`badge ${className}`}>
      {status}
    </span>
  );
}
