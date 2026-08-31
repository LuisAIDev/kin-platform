package com.kinplatform.kin.health.scheduling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de agenda y disponibilidad (ADR-034).
 *
 * <p>Master switch y anticipación (horas) del recordatorio de citas. Defaults:
 * habilitado.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_SCHEDULING_ENABLED} y
 * {@code KIN_HEALTH_SCHEDULING_REMINDER_HOURS_BEFORE}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.scheduling")
public class SchedulingProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Anticipación (horas) del recordatorio de citas confirmadas (default: 24). */
    private int reminderHoursBefore = 24;

    /** Horas sin confirmar tras las que se recuerda al médico (default: 24). */
    private int confirmationReminderHours = 24;
}
