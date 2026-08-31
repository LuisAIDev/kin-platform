package com.kinplatform.kin.health.followup.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Plan de seguimiento creado por un médico para un paciente con relación
 * {@code ACTIVE} (ADR-033).
 *
 * <p>Contiene un título, descripción, ventana temporal y frecuencia
 * (diaria/semanal/mensual) que determinan las tareas iniciales. Entidad de
 * dominio inmutable; las transiciones de estado producen nuevas instancias.</p>
 */
public record FollowUpPlan(
        UUID id,
        UUID physicianId,
        UUID patientId,
        String title,
        String description,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        FollowUpFrequency frequency,
        FollowUpStatus status,
        OffsetDateTime createdAt) {

    public FollowUpPlan {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (physicianId == null || patientId == null) {
            throw new IllegalArgumentException("physicianId/patientId no pueden ser null");
        }
        title = title == null ? "" : title;
        description = description == null ? "" : description;
        startDate = startDate == null ? OffsetDateTime.now() : startDate;
        frequency = frequency == null ? FollowUpFrequency.WEEKLY : frequency;
        status = status == null ? FollowUpStatus.ACTIVE : status;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static FollowUpPlan of(
            UUID id,
            UUID physicianId,
            UUID patientId,
            String title,
            String description,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            FollowUpFrequency frequency,
            FollowUpStatus status,
            OffsetDateTime createdAt) {
        return new FollowUpPlan(
                id, physicianId, patientId, title, description, startDate, endDate, frequency, status, createdAt);
    }

    public boolean involves(UUID userId) {
        return physicianId.equals(userId) || patientId.equals(userId);
    }

    public boolean isActive() {
        return status == FollowUpStatus.ACTIVE;
    }
}
