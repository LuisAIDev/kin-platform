package com.kinplatform.kin.engine;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.scoring.ScoreResult;

/**
 * Contrato común de entrada de los motores que consumen la información ya
 * producida por Java en la conversación: contexto del proyecto, evaluación de
 * completitud, decisión y score.
 *
 * <p>Mantiene tipado fuerte: cada implementación concreta (por ejemplo
 * {@code RecommendationInput}) conserva sus métodos tipados y este contrato
 * garantiza que el pipeline y el {@link EngineExecutor} puedan construir y
 * consumir entradas sin acoplarse a un motor específico.</p>
 */
public interface EngineInput {

    ProjectContext projectContext();

    CompletenessEvaluation evaluation();

    ConversationDecision decision();

    ScoreResult score();
}
