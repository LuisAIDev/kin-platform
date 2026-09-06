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

    /**
     * Bypass de la cuota de invitaciones (plan SALUD_PROFESIONAL y límite de
     * pacientes). Cuando es {@code true}, el médico puede invitar sin
     * suscripción elegible ni límite de pacientes. Variables de entorno:
     * {@code KIN_HEALTH_PHYSICIAN_ALLOW_UNLIMITED_INVITES} o
     * {@code ALLOW_UNLIMITED_INVITES}.
     */
    private boolean allowUnlimitedInvites = false;

    /**
     * Master switch de la cuota de invitaciones (fase piloto). Default
     * {@code false} = cuota DESACTIVADA (las invitaciones no exigen plan ni
     * límite, sin depender de otras variables). Al pasar a GA fijar
     * {@code KIN_HEALTH_PHYSICIAN_ENFORCE_INVITE_QUOTA=true} (o el alias
     * {@code KIN_ENFORCE_PHYSICIAN_INVITE_QUOTA=true}) para volver a exigir el
     * plan Profesional y el límite de pacientes.
     */
    private boolean enforceInviteQuota = false;
}
