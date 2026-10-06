package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.engine.EngineInput;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.Roadmap;
import com.kinplatform.platform.reporting.RecommendationResult;

/**
 * Entrada tipada del {@code RoadmapEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor de hoja de ruta consume para construir el
 * {@link Roadmap}: el contexto del proyecto y las recomendaciones del pipeline.
 * El plan financiero se usa para alinear los hitos (meses relativos).</p>
 */
public record RoadmapInput(
    ProjectContext context,
    RecommendationResult recommendations,
    FinancialPlan financialPlan
) implements EngineInput {
}



