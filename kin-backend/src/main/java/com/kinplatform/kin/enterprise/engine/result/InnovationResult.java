package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;

/**
 * Resultado del {@code InnovationEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link InnovationPlan} producido por el motor de
 * innovación junto con la trazabilidad común de {@link EngineResult}. El
 * Milestone 2A define únicamente el contrato; el plan de innovación real se
 * producirá en el Milestone 2.</p>
 */
public record InnovationResult(
    InnovationPlan plan,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public InnovationResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return plan == null;
    }

    public static InnovationResult empty() {
        return new InnovationResult(null, 0.0, "", "", "");
    }
}
