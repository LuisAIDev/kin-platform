package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.scoring.ScoreResult;

/**
 * Entrada tipada para los OpportunityAnalyzers. Solo consume información
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
public record OpportunityInput(
    ProjectContext projectContext,
    CompletenessEvaluation evaluation,
    ConversationDecision decision,
    ScoreResult score,
    EnrichmentResult enrichment
) implements EngineInput {

    public OpportunityInput {
        enrichment = enrichment == null ? EnrichmentResult.empty() : enrichment;
    }

    public OpportunityInput(ProjectContext projectContext, CompletenessEvaluation evaluation,
                            ConversationDecision decision, ScoreResult score) {
        this(projectContext, evaluation, decision, score, EnrichmentResult.empty());
    }

    public OpportunityInput withEnrichment(EnrichmentResult enrichment) {
        return new OpportunityInput(projectContext, evaluation, decision, score, enrichment);
    }
}
