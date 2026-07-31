package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;

import java.util.UUID;

/**
 * Entrada del {@link ReportEngine}: porta los resultados YA calculados por el
 * pipeline (score, recomendaciones, riesgos y oportunidades) junto con el
 * contexto, la evaluación y la decisión. El motor los consume sin re-ejecutar
 * ningún motor.
 *
 * <p>Evolución prevista (KIN 3.0 / Fase 6): al llegar el 5º motor de
 * resultados, los resultados se encapsularán en un contenedor
 * {@code EngineResults} para no añadir campos al record por cada motor nuevo.
 * Único punto de cambio: la {@code inputFactory} de {@code ReportStage}.</p>
 */
public record ReportInput(
    UUID projectId,
    String projectTitle,
    String projectCategory,
    ProjectContext projectContext,
    CompletenessEvaluation evaluation,
    ConversationDecision decision,
    ScoreResult score,
    RecommendationResult recommendation,
    RiskResult risk,
    OpportunityResult opportunity
) implements EngineInput {
}
