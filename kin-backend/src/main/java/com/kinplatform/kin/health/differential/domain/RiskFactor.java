package com.kinplatform.kin.health.differential.domain;

import java.util.UUID;

/**
 * Factor de riesgo de una condición (ADR-029).
 *
 * <p>Entidad de dominio inmutable: un factor de riesgo (p. ej. {@code fumador})
 * asociado a una condición, con un {@code weight} (0..1) que modifica la
 * probabilidad en el {@code DifferentialEngine}. El peso es aditivo y
 * determinista; nunca lo decide el LLM.</p>
 */
public record RiskFactor(UUID id, UUID conditionId, String factor, double weight, String description) {

    public RiskFactor {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (conditionId == null) {
            throw new IllegalArgumentException("conditionId no puede ser null");
        }
        if (factor == null || factor.isBlank()) {
            throw new IllegalArgumentException("factor no puede ser null o vacío");
        }
        weight = Math.max(0.0, Math.min(1.0, weight));
        description = description == null ? "" : description;
    }

    public static RiskFactor of(UUID id, UUID conditionId, String factor, double weight, String description) {
        return new RiskFactor(id, conditionId, factor, weight, description);
    }
}
