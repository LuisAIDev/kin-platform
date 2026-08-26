package com.kinplatform.kin.health.dashboard.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recordatorio base del paciente (ADR-030).
 *
 * <p>Estructura mínima para futuras citas o medicación: tipo (CITA, MEDICACION,
 * GENERAL), título y fecha programada. Es una base para la agenda del
 * paciente; el módulo la persiste y lista, sin lógica compleja todavía.</p>
 */
public record Reminder(
        UUID id,
        UUID userId,
        ReminderType type,
        String title,
        OffsetDateTime scheduledAt,
        boolean active,
        OffsetDateTime createdAt) {

    public Reminder {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        type = type == null ? ReminderType.GENERAL : type;
        title = title == null ? "" : title;
        scheduledAt = scheduledAt == null ? OffsetDateTime.now() : scheduledAt;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static Reminder of(
            UUID id,
            UUID userId,
            ReminderType type,
            String title,
            OffsetDateTime scheduledAt,
            boolean active,
            OffsetDateTime createdAt) {
        return new Reminder(id, userId, type, title, scheduledAt, active, createdAt);
    }

    public enum ReminderType {
        CITA,
        MEDICACION,
        GENERAL
    }
}
