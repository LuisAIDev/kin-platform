package com.kinplatform.kin.health.followup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de seguimiento de pacientes (ADR-033).
 *
 * <p>Master switch, plazo de recordatorios y días sin evolución para la alerta
 * al médico. Defaults: habilitado.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_FOLLOWUP_ENABLED},
 * {@code KIN_HEALTH_FOLLOWUP_REMINDER_DAYS_BEFORE} y
 * {@code KIN_HEALTH_FOLLOWUP_EVOLUTION_ALERT_DAYS}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.followup")
public class FollowUpProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Anticipación (días) del recordatorio automático de tareas (default: 1). */
    private int reminderDaysBefore = 1;

    /** Días sin evolución registrada que disparan una alerta al médico (default: 7). */
    private int evolutionAlertDays = 7;
}
