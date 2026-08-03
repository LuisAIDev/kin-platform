package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;

/**
 * Resultado del {@code EnterpriseScoreEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link EnterpriseScore} producido por el motor de
 * puntuación junto con la trazabilidad común de {@link EngineResult}. El
 * Milestone 2A define únicamente el contrato; el Enterprise Score real se
 * calculará en el Milestone 2.</p>
 */
public record EnterpriseScoreResult(
    EnterpriseScore score,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public EnterpriseScoreResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return score == null;
    }

    public static EnterpriseScoreResult empty() {
        return new EnterpriseScoreResult(null, 0.0, "", "", "");
    }
}
