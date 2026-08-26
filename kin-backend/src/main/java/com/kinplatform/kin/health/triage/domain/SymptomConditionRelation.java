package com.kinplatform.kin.health.triage.domain;

import java.util.UUID;

/**
 * Relación síntoma ↔ condición del catálogo de triaje (ADR-028).
 *
 * <p>Asocia un síntoma con una condición con un {@code weight} entre 0 y 1 que
 * indica la relevancia del síntoma para esa condición. {@code required} marca
 * los síntomas obligatorios: si un síntoma requerido no está presente, la
 * condición no es candidata (regla determinista del {@code TriageEngine}).</p>
 */
public record SymptomConditionRelation(UUID symptomId, UUID conditionId, double weight, boolean required) {

    public SymptomConditionRelation {
        if (symptomId == null) {
            throw new IllegalArgumentException("symptomId no puede ser null");
        }
        if (conditionId == null) {
            throw new IllegalArgumentException("conditionId no puede ser null");
        }
        weight = Math.max(0.0, Math.min(1.0, weight));
    }

    public static SymptomConditionRelation of(UUID symptomId, UUID conditionId, double weight, boolean required) {
        return new SymptomConditionRelation(symptomId, conditionId, weight, required);
    }
}
