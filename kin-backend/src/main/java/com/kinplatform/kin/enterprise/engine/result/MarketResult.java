package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;

/**
 * Resultado del {@code MarketEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link MarketPlan} producido por el motor de mercado
 * junto con la trazabilidad común de {@link EngineResult}. El Milestone 2A
 * define únicamente el contrato; el plan de mercado real se producirá en el
 * Milestone 2.</p>
 */
public record MarketResult(
    MarketPlan plan,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public MarketResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return plan == null;
    }

    public static MarketResult empty() {
        return new MarketResult(null, 0.0, "", "", "");
    }
}
