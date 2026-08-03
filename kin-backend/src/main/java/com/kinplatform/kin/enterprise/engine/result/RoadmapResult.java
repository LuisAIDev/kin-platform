package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.Roadmap;

/**
 * Resultado del {@code RoadmapEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link Roadmap} producido por el motor de hoja de
 * ruta junto con la trazabilidad común de {@link EngineResult}. El Milestone 2A
 * define únicamente el contrato; el roadmap real se producirá en el
 * Milestone 2.</p>
 */
public record RoadmapResult(
    Roadmap roadmap,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public RoadmapResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return roadmap == null;
    }

    public static RoadmapResult empty() {
        return new RoadmapResult(null, 0.0, "", "", "");
    }
}
