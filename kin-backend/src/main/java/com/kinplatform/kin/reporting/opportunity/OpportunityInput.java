package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.scoring.ScoreResult;

/**
 * Entrada tipada para los OpportunityAnalyzers. Solo consume información
 * producida por Java: contexto del proyecto, evaluación de completitud,
 * decisión de conversación y score de viabilidad.
 *
 * <p>Implementa {@link EngineInput} para integrarse con la infraestructura
 * común de motores manteniendo tipado fuerte.</p>
 */
public record OpportunityInput(
    ProjectContext projectContext,
    CompletenessEvaluation evaluation,
    ConversationDecision decision,
    ScoreResult score
) implements EngineInput {}
