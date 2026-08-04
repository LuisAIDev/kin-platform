interface ProgressBarProps {
  /** Progreso porcentual (0-100). */
  progress: number;
  /** Estado de la generación (para el color). */
  status?: string;
  /** Etiqueta opcional junto al porcentaje. */
  label?: string;
}

/** Barra de progreso de la generación Enterprise. */
export function ProgressBar({ progress, status, label }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, progress));
  const statusClass =
    status === "COMPLETED" || status === "FAILED"
      ? `progress-fill-${status}`
      : "";
  return (
    <div data-testid="progress-bar">
      <div
        className="progress-track"
        role="progressbar"
        aria-valuenow={clamped}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className={`progress-fill ${statusClass}`.trim()}
          style={{ width: `${clamped}%` }}
        />
      </div>
      <div className="progress-label">
        <span>{label ?? "Progreso"}</span>
        <span>{clamped}%</span>
      </div>
    </div>
  );
}
