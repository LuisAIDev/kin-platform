package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.LeanCanvas;

/**
 * Resultado del {@code BusinessModelEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link LeanCanvas} producido por el motor de modelo
 * de negocio junto con la trazabilidad común de {@link EngineResult}. El
 * Milestone 2A define únicamente el contrato; el Lean Canvas real se producirá
 * en el Milestone 2.</p>
 */
public record BusinessModelResult(
    LeanCanvas canvas,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public BusinessModelResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return canvas == null;
    }

    public static BusinessModelResult empty() {
        return new BusinessModelResult(null, 0.0, "", "", "");
    }
}
