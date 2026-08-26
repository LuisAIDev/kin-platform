package com.kinplatform.kin.health.dashboard.api;

import com.kinplatform.kin.health.dashboard.domain.Reminder;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recordatorio en el endpoint REST (ADR-030).
 */
public record ReminderResponse(
        UUID id, String type, String title, OffsetDateTime scheduledAt, boolean active, OffsetDateTime createdAt) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.id(),
                reminder.type().name(),
                reminder.title(),
                reminder.scheduledAt(),
                reminder.active(),
                reminder.createdAt());
    }
}
