package com.kinplatform.kin.reporting;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.scoring.ScoreResult;

/**
 * Entrada tipada para el RecommendationEngine. Solo consume información
 * producida por Java: contexto del proyecto, evaluación de completitud,
 * decisión de conversación, score de viabilidad y, opcionalmente, el resultado
 * de enriquecimiento con conocimiento externo (ADR-016).
 *
 * <p>El enriquecimiento es aditivo: los constructores de 4 parámetros y el
 * acceso a {@code enrichment()} conservan el comportamiento anterior cuando no
 * hay hechos (el compact constructor normaliza {@code null} a
 * {@link EnrichmentResult#empty()}).</p>
 *
 * <p>Implementa {@link EngineInput} para integrarse con la infraestructura
 * común de motores manteniendo tipado fuerte.</p>
 */
public record RecommendationInput(
    ProjectContext projectContext,
    CompletenessEvaluation evaluation,
    ConversationDecision decision,
    ScoreResult score,
    EnrichmentResult enrichment
) implements EngineInput {

    public RecommendationInput {
        enrichment = enrichment == null ? EnrichmentResult.empty() : enrichment;
    }

    public RecommendationInput(ProjectContext projectContext, CompletenessEvaluation evaluation,
                               ConversationDecision decision, ScoreResult score) {
        this(projectContext, evaluation, decision, score, EnrichmentResult.empty());
    }

    public RecommendationInput withEnrichment(EnrichmentResult enrichment) {
        return new RecommendationInput(projectContext, evaluation, decision, score, enrichment);
    }
}
