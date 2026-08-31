package com.kinplatform.kin.health.physician.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de portal de médicos (ADR-031).
 *
 * <p>Master switch del módulo, switch del flujo de invitación de pacientes y
 * switch del correo de notificación de invitación. Defaults: habilitado.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_PHYSICIAN_ENABLED},
 * {@code KIN_HEALTH_PHYSICIAN_INVITE_ENABLED} y
 * {@code KIN_HEALTH_PHYSICIAN_INVITATION_EMAIL_ENABLED}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.physician")
public class PhysicianProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Habilita el flujo de invitación/aceptación de relaciones (V30). */
    private boolean inviteEnabled = true;

    /** Habilita el correo de notificación al paciente cuando recibe una invitación. */
    private boolean invitationEmailEnabled = true;
}
