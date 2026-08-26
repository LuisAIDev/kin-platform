package com.kinplatform.kin.health.triage.domain;

import java.util.List;
import java.util.UUID;

/**
 * Resultado por condición del motor de triaje (ADR-028).
 *
 * <p>Incluye el id/nombre de la condición, la probabilidad (score 0..1), la
 * severidad, la urgencia, la recomendación breve y los síntomas que
 * coincidieron (trazabilidad del cálculo).</p>
 */
public record TriageConditionResult(
        UUID conditionId,
        String name,
        String description,
        double probability,
        Severity severity,
        Urgency urgency,
        String recommendation,
        List<String> matchedSymptoms) {

    public TriageConditionResult {
        probability = Math.max(0.0, Math.min(1.0, probability));
        description = description == null ? "" : description;
        recommendation = recommendation == null ? "" : recommendation;
        matchedSymptoms = matchedSymptoms == null ? List.of() : List.copyOf(matchedSymptoms);
    }
}
