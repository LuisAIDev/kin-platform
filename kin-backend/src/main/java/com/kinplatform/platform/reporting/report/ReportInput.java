package com.kinplatform.platform.reporting.report;

import com.kinplatform.common.context.CompletenessEvaluation;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.common.engine.EngineInput;
import com.kinplatform.platform.enrichment.EnrichmentResult;
import com.kinplatform.platform.reporting.RecommendationResult;
import com.kinplatform.platform.reporting.opportunity.OpportunityResult;
import com.kinplatform.platform.reporting.risk.RiskResult;
import com.kinplatform.platform.scoring.ScoreResult;

import java.util.UUID;

/**
 * Entrada del {@link ReportEngine}: porta los resultados YA calculados por el
 * pipeline (score, recomendaciones, riesgos y oportunidades) junto con el
 * contexto, la evaluación, la decisión y, opcionalmente, el resultado de
 * enriquecimiento con conocimiento externo (ADR-016). El motor los consume sin
 * re-ejecutar ningún motor.
 *
 * <p>El enriquecimiento es aditivo: el constructor de 10 parámetros y el
 * acceso a {@code enrichment()} conservan el comportamiento anterior cuando no
 * hay hechos (el compact constructor normaliza {@code null} a
 * {@link EnrichmentResult#empty()}).</p>
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
    OpportunityResult opportunity,
    EnrichmentResult enrichment
) implements EngineInput {

    public ReportInput {
        enrichment = enrichment == null ? EnrichmentResult.empty() : enrichment;
    }

    public ReportInput(UUID projectId, String projectTitle, String projectCategory,
                       ProjectContext projectContext, CompletenessEvaluation evaluation,
                       ConversationDecision decision, ScoreResult score,
                       RecommendationResult recommendation, RiskResult risk,
                       OpportunityResult opportunity) {
        this(projectId, projectTitle, projectCategory, projectContext, evaluation, decision,
            score, recommendation, risk, opportunity, EnrichmentResult.empty());
    }

    public ReportInput withEnrichment(EnrichmentResult enrichment) {
        return new ReportInput(projectId, projectTitle, projectCategory, projectContext,
            evaluation, decision, score, recommendation, risk, opportunity, enrichment);
    }
}






