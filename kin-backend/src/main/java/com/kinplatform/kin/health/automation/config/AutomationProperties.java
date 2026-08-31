package com.kinplatform.kin.health.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del módulo Automation (ADR-038). */
@Data
@ConfigurationProperties(prefix = "kin.health.automation")
public class AutomationProperties {

    /** Master switch del módulo de automatizaciones. */
    private boolean enabled = true;

    /** Límite máximo de reglas por médico (default 50). */
    private int maxRulesPerPhysician = 50;

    /** Tiempo de ejecución máximo en milisegundos (default 30000). */
    private int executionTimeoutMillis = 30000;

    /** Límite máximo de notificaciones por evento (default 3). */
    private int maxNotificationsPerEvent = 3;
}