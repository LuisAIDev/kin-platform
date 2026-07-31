package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.scoring.ScoreResult;

/**
 * Entrada tipada para los RiskAnalyzers. Solo consume información producida
 * por Java: contexto del proyecto, evaluación de completitud, decisión y score.
 *
 * <p>Implementa {@link EngineInput} para integrarse con la infraestructura
 * común de motores manteniendo tipado fuerte.</p>
 */
public record RiskInput(
    ProjectContext projectContext,
    CompletenessEvaluation evaluation,
    ConversationDecision decision,
    ScoreResult score
) implements EngineInput {}
