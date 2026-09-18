package com.kinplatform.kin.health.triage.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consulta de triaje persistida (ADR-028).
 *
 * <p>Registro auditable de una consulta realizada por un paciente: síntomas
 * reportados, resultados obtenidos y fecha. Permite el historial por usuario
 * y cumple el requisito de confidencialidad (aislamiento por {@code userId}).</p>
 */
public record TriageConsultation(
        UUID id,
        UUID userId,
        List<String> symptoms,
        List<TriageConditionResult> results,
        OffsetDateTime createdAt,
        OffsetDateTime hiddenAt,
        UUID hiddenBy) {

    public TriageConsultation {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
        results = results == null ? List.of() : List.copyOf(results);
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static TriageConsultation of(
            UUID id,
            UUID userId,
            List<String> symptoms,
            List<TriageConditionResult> results,
            OffsetDateTime createdAt) {
        return new TriageConsultation(id, userId, symptoms, results, createdAt, null, null);
    }

    public static TriageConsultation of(
            UUID id,
            UUID userId,
            List<String> symptoms,
            List<TriageConditionResult> results,
            OffsetDateTime createdAt,
            OffsetDateTime hiddenAt,
            UUID hiddenBy) {
        return new TriageConsultation(id, userId, symptoms, results, createdAt, hiddenAt, hiddenBy);
    }
}
