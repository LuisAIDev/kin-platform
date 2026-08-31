package com.kinplatform.kin.health.telemedicine.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cita de telemedicina / agenda (ADR-032, ampliado por ADR-034).
 *
 * <p>Entidad de dominio inmutable: solicitud de cita de un paciente a su médico
 * asignado, con fecha/hora y motivo. El estado evoluciona de forma
 * determinista: {@code PENDIENTE} → {@code CONFIRMADA}/{@code CANCELADA} →
 * {@code COMPLETADA}; la reprogramación marca la cita anterior como
 * {@code REPROGRAMADA} y crea una nueva con referencia en
 * {@code rescheduledFrom}.</p>
 */
public record Appointment(
        UUID id,
        UUID patientId,
        UUID physicianId,
        OffsetDateTime scheduledAt,
        int durationMinutes,
        String reason,
        AppointmentStatus status,
        OffsetDateTime createdAt,
        UUID rescheduledFrom,
        String cancellationReason,
        UUID availabilitySlotId,
        boolean reminderSent) {

    public Appointment {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (patientId == null || physicianId == null) {
            throw new IllegalArgumentException("patientId/physicianId no pueden ser null");
        }
        durationMinutes = durationMinutes <= 0 ? 30 : durationMinutes;
        reason = reason == null ? "" : reason;
        status = status == null ? AppointmentStatus.PENDIENTE : status;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        cancellationReason = cancellationReason == null ? "" : cancellationReason;
    }

    /** Factory de compatibilidad (contrato ADR-032); delega con valores por defecto. */
    public static Appointment of(
            UUID id,
            UUID patientId,
            UUID physicianId,
            OffsetDateTime scheduledAt,
            String reason,
            AppointmentStatus status,
            OffsetDateTime createdAt) {
        return new Appointment(
                id, patientId, physicianId, scheduledAt, 30, reason, status, createdAt, null, "", null, false);
    }

    /** Factory completa (ADR-034). */
    public static Appointment of(
            UUID id,
            UUID patientId,
            UUID physicianId,
            OffsetDateTime scheduledAt,
            int durationMinutes,
            String reason,
            AppointmentStatus status,
            OffsetDateTime createdAt,
            UUID rescheduledFrom,
            String cancellationReason,
            UUID availabilitySlotId,
            boolean reminderSent) {
        return new Appointment(
                id, patientId, physicianId, scheduledAt, durationMinutes, reason, status, createdAt,
                rescheduledFrom, cancellationReason, availabilitySlotId, reminderSent);
    }

    public boolean involves(UUID userId) {
        return patientId.equals(userId) || physicianId.equals(userId);
    }

    public boolean isOpen() {
        return status == AppointmentStatus.PENDIENTE || status == AppointmentStatus.CONFIRMADA;
    }

    /** Fin de la cita (para validar traslapes). */
    public OffsetDateTime endAt() {
        return scheduledAt.plusMinutes(durationMinutes);
    }

    /** Confirma la cita (médico). */
    public Appointment confirmed() {
        return new Appointment(id, patientId, physicianId, scheduledAt, durationMinutes, reason,
                AppointmentStatus.CONFIRMADA, createdAt, rescheduledFrom, cancellationReason, availabilitySlotId,
                reminderSent);
    }

    /** Cancela la cita con motivo (paciente o médico). */
    public Appointment canceled(String cancellation) {
        return new Appointment(id, patientId, physicianId, scheduledAt, durationMinutes, reason,
                AppointmentStatus.CANCELADA, createdAt, rescheduledFrom, cancellation == null ? "" : cancellation,
                availabilitySlotId, reminderSent);
    }

    /** Reprograma: la cita actual pasa a REPROGRAMADA; se crea la nueva aparte. */
    public Appointment rescheduled() {
        return new Appointment(id, patientId, physicianId, scheduledAt, durationMinutes, reason,
                AppointmentStatus.REPROGRAMADA, createdAt, rescheduledFrom, cancellationReason, availabilitySlotId,
                reminderSent);
    }

    /** Marca la cita como completada (médico). */
    public Appointment completed() {
        return new Appointment(id, patientId, physicianId, scheduledAt, durationMinutes, reason,
                AppointmentStatus.COMPLETADA, createdAt, rescheduledFrom, cancellationReason, availabilitySlotId,
                reminderSent);
    }

    /** Registra que el recordatorio automático ya se envió. */
    public Appointment withReminderSent() {
        return new Appointment(id, patientId, physicianId, scheduledAt, durationMinutes, reason, status, createdAt,
                rescheduledFrom, cancellationReason, availabilitySlotId, true);
    }

    public enum AppointmentStatus {
        PENDIENTE,
        CONFIRMADA,
        CANCELADA,
        COMPLETADA,
        REPROGRAMADA
    }
}
