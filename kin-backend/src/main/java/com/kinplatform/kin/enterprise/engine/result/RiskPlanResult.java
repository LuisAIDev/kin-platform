package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.RiskMatrix;

/**
 * Resultado del {@code RiskPlanEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link RiskMatrix} producido por el motor de matriz
 * de riesgos junto con la trazabilidad común de {@link EngineResult}. El
 * Milestone 2A define únicamente el contrato; la matriz real se producirá en el
 * Milestone 2.</p>
 */
public record RiskPlanResult(
    RiskMatrix matrix,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public RiskPlanResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return matrix == null;
    }

    public static RiskPlanResult empty() {
        return new RiskPlanResult(null, 0.0, "", "", "");
    }
}
