package com.kinplatform.kin.health.dashboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de dashboard de salud (ADR-030).
 *
 * <p>Master switch y límite de condiciones frecuentes del resumen. Defaults
 * seguros: módulo habilitado.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_DASHBOARD_ENABLED} y
 * {@code KIN_HEALTH_DASHBOARD_TOP_CONDITIONS}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.dashboard")
public class DashboardProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Límite de condiciones más frecuentes en el resumen (default 3). */
    private int topConditions = 3;
}
