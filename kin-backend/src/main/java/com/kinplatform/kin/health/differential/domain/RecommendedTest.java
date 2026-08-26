package com.kinplatform.kin.health.differential.domain;

import java.util.UUID;

/**
 * Prueba complementaria recomendada para una condición (ADR-029).
 *
 * <p>Entidad de dominio inmutable: sugiere una prueba para diferenciar una
 * condición candidata de otras del diagnóstico diferencial. Seleccionada en
 * Java por el {@code DifferentialEngine} según las condiciones con mayor
 * probabilidad.</p>
 */
public record RecommendedTest(UUID id, UUID conditionId, String test, String description) {

    public RecommendedTest {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (conditionId == null) {
            throw new IllegalArgumentException("conditionId no puede ser null");
        }
        if (test == null || test.isBlank()) {
            throw new IllegalArgumentException("test no puede ser null o vacío");
        }
        description = description == null ? "" : description;
    }

    public static RecommendedTest of(UUID id, UUID conditionId, String test, String description) {
        return new RecommendedTest(id, conditionId, test, description);
    }
}
