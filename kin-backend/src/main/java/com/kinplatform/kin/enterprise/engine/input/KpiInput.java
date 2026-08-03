package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.KpiSet;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;

/**
 * Entrada tipada del {@code KpiEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor de KPIs consume para construir el
 * {@link KpiSet}: el contexto del proyecto, el plan de mercado (objetivos de
 * ingresos) y el plan financiero (márgenes y proyecciones).</p>
 */
public record KpiInput(
    ProjectContext context,
    MarketPlan marketPlan,
    FinancialPlan financialPlan
) implements EngineInput {
}
