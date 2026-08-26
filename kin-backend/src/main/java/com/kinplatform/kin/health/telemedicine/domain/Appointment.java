package com.kinplatform.kin.health.telemedicine.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cita de telemedicina (ADR-032).
 *
 * <p>Entidad de dominio inmutable: solicitud de cita de un paciente a su médico
 * asignado, con fecha/hora y motivo. El estado evoluciona de forma
 * determinista: {@code PENDIENTE} → {@code CONFIRMADA}/{@code CANCELADA} →
 * {@code COMPLETADA}.</p>
 */
public record Appointment(
        UUID id,
        UUID patientId,
        UUID physicianId,
        OffsetDateTime scheduledAt,
        String reason,
        AppointmentStatus status,
        OffsetDateTime createdAt) {

    public Appointment {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (patientId == null || physicianId == null) {
            throw new IllegalArgumentException("patientId/physicianId no pueden ser null");
        }
        reason = reason == null ? "" : reason;
        status = status == null ? AppointmentStatus.PENDIENTE : status;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static Appointment of(
            UUID id,
            UUID patientId,
            UUID physicianId,
            OffsetDateTime scheduledAt,
            String reason,
            AppointmentStatus status,
            OffsetDateTime createdAt) {
        return new Appointment(id, patientId, physicianId, scheduledAt, reason, status, createdAt);
    }

    public boolean involves(UUID userId) {
        return patientId.equals(userId) || physicianId.equals(userId);
    }

    public enum AppointmentStatus {
        PENDIENTE,
        CONFIRMADA,
        CANCELADA,
        COMPLETADA
    }
}
