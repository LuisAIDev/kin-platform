package com.kinplatform.kin.health.triage.domain;

import com.kinplatform.kin.engine.EngineResult;
import java.util.List;

/**
 * Resultado inmutable del motor de triaje (ADR-028).
 *
 * <p>Lista de condiciones candidatas ordenadas por probabilidad descendente,
 * síntomas no reconocidos (que no están en el catálogo) y trazabilidad
 * (generador + versión). Implementa {@link EngineResult} para compartir el
 * contrato común de resultados; {@code empty()} permite el modo seguro cuando
 * no hay síntomas reconocidos o el catálogo está vacío.</p>
 */
public record TriageResult(
        List<TriageConditionResult> results,
        List<String> unrecognizedSymptoms,
        double confidence,
        String explanation,
        String generatedBy,
        String engineVersion)
        implements EngineResult {

    public TriageResult {
        results = results == null ? List.of() : List.copyOf(results);
        unrecognizedSymptoms = unrecognizedSymptoms == null ? List.of() : List.copyOf(unrecognizedSymptoms);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        explanation = explanation == null ? "" : explanation;
        generatedBy = generatedBy == null ? "" : generatedBy;
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    public static TriageResult empty() {
        return new TriageResult(List.of(), List.of(), 0.0, "No hay síntomas reconocidos para el triaje.", "", "");
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }
}
