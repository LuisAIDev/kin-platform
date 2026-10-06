package com.kinplatform.common.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de auditoría (ADR-035).
 *
 * <p>Master switch y retención de logs. Defaults: habilitado, 365 días.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_AUDIT_ENABLED} y
 * {@code KIN_HEALTH_AUDIT_RETENTION_DAYS}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.audit")
public class AuditProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Retención de logs en días (purga automática opcional). */
    private int retentionDays = 365;
}

