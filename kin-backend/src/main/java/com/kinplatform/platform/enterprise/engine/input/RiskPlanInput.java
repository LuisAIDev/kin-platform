package com.kinplatform.platform.enterprise.engine.input;

import com.kinplatform.common.engine.EngineInput;
import com.kinplatform.platform.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.platform.enterprise.valueobjects.RiskMatrix;
import com.kinplatform.platform.reporting.risk.RiskResult;

/**
 * Entrada tipada del {@code RiskPlanEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor de matriz de riesgos consume para transformar
 * el {@link RiskResult} del pipeline en una {@link RiskMatrix} de presentación:
 * el resultado de riesgo ya calculado y, opcionalmente, el plan financiero para
 * derivar el riesgo financiero adicional.</p>
 */
public record RiskPlanInput(
    RiskResult riskResult,
    FinancialPlan financialPlan
) implements EngineInput {
}




