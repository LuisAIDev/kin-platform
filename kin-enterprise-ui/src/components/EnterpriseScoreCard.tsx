import type { EnterpriseScoreSection } from "../types/enterprise";

interface EnterpriseScoreCardProps {
  /** Enterprise Score de la versión (null si no está disponible). */
  score?: EnterpriseScoreSection | null;
}

const DIMENSIONS = [
  ["market", "Mercado"],
  ["innovation", "Innovación"],
  ["viability", "Viabilidad"],
  ["financial", "Finanzas"],
  ["risk", "Riesgo"],
  ["scalability", "Escalabilidad"],
  ["team", "Equipo"],
  ["sustainability", "Sostenibilidad"],
] as const;

/** Tarjeta del Enterprise Score (o estado "Pendiente" si no está disponible). */
export function EnterpriseScoreCard({ score }: EnterpriseScoreCardProps) {
  if (!score || score.overall == null) {
    return (
      <div data-testid="score-card">
        <div className="card-title">Enterprise Score</div>
        <p className="score-empty">Pendiente de generación.</p>
      </div>
    );
  }
  return (
    <div data-testid="score-card">
      <div className="card-title">Enterprise Score</div>
      <div>
        <span className="score-value">{score.overall}</span>
        <span className="score-grade"> / 100 · {score.grade ?? "—"}</span>
      </div>
      <div className="score-dims">
        {DIMENSIONS.map(([key, label]) => (
          <span key={key}>
            {label}: {score[key] ?? "—"}
          </span>
        ))}
      </div>
    </div>
  );
}
