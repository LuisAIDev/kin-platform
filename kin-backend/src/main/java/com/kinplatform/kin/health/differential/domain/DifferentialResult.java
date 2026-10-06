package com.kinplatform.kin.health.differential.domain;

import com.kinplatform.common.engine.EngineResult;
import java.util.List;

/**
 * Resultado inmutable del motor de diagnóstico diferencial (ADR-029).
 *
 * <p>Lista de {@link DifferentialItem} ordenada por probabilidad descendente
 * (probabilidad base del triaje ajustada por factores de riesgo), con pruebas
 * recomendadas para diferenciar. Implementa {@link EngineResult}; {@code empty()}
 * permite el modo seguro cuando no hay condiciones candidatas.</p>
 */
public record DifferentialResult(
        List<DifferentialItem> items,
        List<String> unrecognizedSymptoms,
        double confidence,
        String explanation,
        String generatedBy,
        String engineVersion)
        implements EngineResult {

    public DifferentialResult {
        items = items == null ? List.of() : List.copyOf(items);
        unrecognizedSymptoms = unrecognizedSymptoms == null ? List.of() : List.copyOf(unrecognizedSymptoms);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        explanation = explanation == null ? "" : explanation;
        generatedBy = generatedBy == null ? "" : generatedBy;
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    public static DifferentialResult empty() {
        return new DifferentialResult(
                List.of(), List.of(), 0.0, "No hay condiciones para el diagnóstico diferencial.", "", "");
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }
}

