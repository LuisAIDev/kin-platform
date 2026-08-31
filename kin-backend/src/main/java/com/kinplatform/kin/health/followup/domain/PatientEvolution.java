package com.kinplatform.kin.health.followup.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Registro de evolución del paciente (reporte periódico del médico, ADR-033).
 *
 * <p>Incluye síntomas (texto libre), signos vitales en un {@code Map} flexible
 * (JSONB en persistencia, p. ej. presión, peso, frecuencia cardíaca), adherencia
 * a la medicación y notas. Solo el médico con relación {@code ACTIVE} puede
 * registrarla; el paciente la consulta en modo lectura.</p>
 */
public record PatientEvolution(
        UUID id,
        UUID patientId,
        UUID physicianId,
        OffsetDateTime recordedAt,
        String symptoms,
        Map<String, Object> vitals,
        Boolean medicationAdherence,
        String notes,
        OffsetDateTime createdAt) {

    public PatientEvolution {
        if (id == null || patientId == null || physicianId == null) {
            throw new IllegalArgumentException("id/patientId/physicianId no pueden ser null");
        }
        recordedAt = recordedAt == null ? OffsetDateTime.now() : recordedAt;
        symptoms = symptoms == null ? "" : symptoms;
        vitals = vitals == null ? Map.of() : Map.copyOf(vitals);
        notes = notes == null ? "" : notes;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static PatientEvolution of(
            UUID id,
            UUID patientId,
            UUID physicianId,
            OffsetDateTime recordedAt,
            String symptoms,
            Map<String, Object> vitals,
            Boolean medicationAdherence,
            String notes,
            OffsetDateTime createdAt) {
        return new PatientEvolution(
                id, patientId, physicianId, recordedAt, symptoms, vitals, medicationAdherence, notes, createdAt);
    }
}
