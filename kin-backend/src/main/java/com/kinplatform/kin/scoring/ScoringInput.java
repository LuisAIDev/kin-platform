package com.kinplatform.kin.scoring;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada para el ScoringEngine. Solo consume la información producida
 * por Java: el contexto del proyecto y la evaluación de completitud.
 *
 * <p>Implementa {@link EngineInput} (marcador) para integrarse con la
 * infraestructura común de motores manteniendo tipado fuerte.</p>
 */
public record ScoringInput(
    ProjectContext projectContext,
    CompletenessEvaluation evaluation
) implements EngineInput {
}
