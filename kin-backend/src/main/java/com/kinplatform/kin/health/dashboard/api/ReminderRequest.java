package com.kinplatform.kin.health.dashboard.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Request de creación de recordatorio (ADR-030).
 *
 * @param type        tipo del recordatorio (CITA, MEDICACION, GENERAL)
 * @param title       título del recordatorio
 * @param scheduledAt fecha programada
 */
public record ReminderRequest(
        @NotBlank(message = "El tipo es obligatorio") String type,
        @NotBlank(message = "El título es obligatorio")
                @Size(max = 160, message = "El título no debe superar 160 caracteres")
                String title,
        @NotNull(message = "La fecha programada es obligatoria") OffsetDateTime scheduledAt) {}
