package com.kinplatform.kin.health.physician.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de portal de médicos (ADR-031).
 *
 * <p>Master switch del módulo. Default: habilitado.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_PHYSICIAN_ENABLED}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.physician")
public class PhysicianProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;
}
