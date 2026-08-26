package com.kinplatform.kin.health.dashboard.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Perfil del paciente (ADR-030).
 *
 * <p>Entidad de dominio inmutable: factores de riesgo (p. ej. {@code fumador},
 * {@code diabetes}) y condiciones crónicas declaradas por el paciente. El
 * {@code DifferentialEngine} (ADR-029) puede consumir los factores de riesgo
 * para ajustar probabilidades en futuras consultas.</p>
 */
public record PatientProfile(
        UUID userId, List<String> riskFactors, List<String> chronicConditions, OffsetDateTime updatedAt) {

    public PatientProfile {
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        chronicConditions = chronicConditions == null ? List.of() : List.copyOf(chronicConditions);
        updatedAt = updatedAt == null ? OffsetDateTime.now() : updatedAt;
    }

    public static PatientProfile empty(UUID userId) {
        return new PatientProfile(userId, List.of(), List.of(), OffsetDateTime.now());
    }

    public static PatientProfile of(
            UUID userId, List<String> riskFactors, List<String> chronicConditions, OffsetDateTime updatedAt) {
        return new PatientProfile(userId, riskFactors, chronicConditions, updatedAt);
    }
}
