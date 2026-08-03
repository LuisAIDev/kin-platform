package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;

/**
 * Resultado del {@code FinancialPlanEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link FinancialPlan} producido por el motor
 * financiero junto con la trazabilidad común de {@link EngineResult}. El
 * Milestone 2A define únicamente el contrato; el plan financiero real se
 * producirá en el Milestone 2.</p>
 */
public record FinancialPlanResult(
    FinancialPlan plan,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public FinancialPlanResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return plan == null;
    }

    public static FinancialPlanResult empty() {
        return new FinancialPlanResult(null, 0.0, "", "", "");
    }
}
