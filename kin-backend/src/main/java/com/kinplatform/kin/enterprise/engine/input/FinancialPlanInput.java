package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.reporting.RecommendationResult;

/**
 * Entrada tipada del {@code FinancialPlanEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor financiero consume para construir el
 * {@link FinancialPlan}: el contexto del proyecto y el plan de mercado
 * (SOM como base de ingresos). Las recomendaciones se usan como entrada
 * adicional de contexto. Todos los cálculos son deterministas.</p>
 */
public record FinancialPlanInput(
    ProjectContext context,
    MarketPlan marketPlan,
    RecommendationResult recommendations
) implements EngineInput {
}
