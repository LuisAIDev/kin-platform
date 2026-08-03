package com.kinplatform.kin.enterprise.engine.result;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.valueobjects.KpiSet;

/**
 * Resultado del {@code KpiEngine} (Fase 10, Milestone 2A).
 *
 * <p>Porta el value object {@link KpiSet} producido por el motor de KPIs junto
 * con la trazabilidad común de {@link EngineResult}. El Milestone 2A define
 * únicamente el contrato; los KPIs reales se producirán en el Milestone 2.</p>
 */
public record KpiResult(
    KpiSet kpis,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public KpiResult {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    @Override
    public boolean isEmpty() {
        return kpis == null;
    }

    public static KpiResult empty() {
        return new KpiResult(null, 0.0, "", "", "");
    }
}
