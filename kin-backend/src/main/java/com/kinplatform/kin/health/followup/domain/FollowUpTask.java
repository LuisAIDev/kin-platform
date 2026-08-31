package com.kinplatform.kin.health.followup.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tarea concreta dentro de un plan de seguimiento (ADR-033).
 *
 * <p>Ejemplos: "Tomar medicación", "Registrar presión arterial". Tiene fecha de
 * vencimiento y estado {@code PENDING}/{@code COMPLETED}/{@code OVERDUE}.
 * {@code reminderSent} evita duplicar recordatorios automáticos del scheduler.</p>
 */
public record FollowUpTask(
        UUID id,
        UUID planId,
        String description,
        OffsetDateTime dueDate,
        FollowUpTaskStatus status,
        OffsetDateTime completedAt,
        boolean reminderSent,
        OffsetDateTime createdAt) {

    public FollowUpTask {
        if (id == null || planId == null) {
            throw new IllegalArgumentException("id/planId no pueden ser null");
        }
        description = description == null ? "" : description;
        dueDate = dueDate == null ? OffsetDateTime.now() : dueDate;
        status = status == null ? FollowUpTaskStatus.PENDING : status;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static FollowUpTask of(
            UUID id,
            UUID planId,
            String description,
            OffsetDateTime dueDate,
            FollowUpTaskStatus status,
            OffsetDateTime completedAt,
            boolean reminderSent,
            OffsetDateTime createdAt) {
        return new FollowUpTask(
                id, planId, description, dueDate, status, completedAt, reminderSent, createdAt);
    }

    public static FollowUpTask pending(UUID id, UUID planId, String description, OffsetDateTime dueDate) {
        return new FollowUpTask(id, planId, description, dueDate, FollowUpTaskStatus.PENDING, null, false,
                OffsetDateTime.now());
    }

    /** Marca la tarea como completada (registra {@code completedAt}). */
    public FollowUpTask completed(OffsetDateTime at) {
        OffsetDateTime done = at == null ? OffsetDateTime.now() : at;
        return new FollowUpTask(
                id, planId, description, dueDate, FollowUpTaskStatus.COMPLETED, done, reminderSent, createdAt);
    }

    /** Marca la tarea como vencida (scheduler diario). */
    public FollowUpTask overdue() {
        return new FollowUpTask(id, planId, description, dueDate, FollowUpTaskStatus.OVERDUE, null, reminderSent, createdAt);
    }

    /** Registra que el recordatorio automático ya se generó. */
    public FollowUpTask withReminderSent() {
        return new FollowUpTask(id, planId, description, dueDate, status, completedAt, true, createdAt);
    }

    public boolean isPending() {
        return status == FollowUpTaskStatus.PENDING;
    }
}
