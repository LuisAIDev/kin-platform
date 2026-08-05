package com.kinplatform.kin.knowledge.policy;

import java.time.Duration;

/**
 * Configuración de políticas de costo (especificación Fase 2): límites de
 * consultas y de llamadas externas por turno, y timeout máximo por consulta.
 * Controla el consumo de recursos operativos de forma determinista.
 *
 * <p>Inmutable y determinista; varía por entorno sin tocar el código.</p>
 */
public record CostPolicyConfig(
    int maxQueries,
    int maxExternalCalls,
    Duration maxTimeout
) {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public CostPolicyConfig {
        maxQueries = Math.max(0, maxQueries);
        maxExternalCalls = Math.max(0, maxExternalCalls);
        maxTimeout = maxTimeout == null ? DEFAULT_TIMEOUT : maxTimeout;
    }

    public static CostPolicyConfig defaults() {
        return new CostPolicyConfig(10, 5, DEFAULT_TIMEOUT);
    }

    public static CostPolicyConfig dev() {
        return new CostPolicyConfig(50, 20, DEFAULT_TIMEOUT);
    }

    public static CostPolicyConfig production() {
        return new CostPolicyConfig(10, 5, Duration.ofSeconds(15));
    }

    public static CostPolicyConfig testing() {
        return new CostPolicyConfig(2, 1, Duration.ofSeconds(1));
    }

    public static CostPolicyConfig enterprise() {
        return new CostPolicyConfig(100, 50, Duration.ofSeconds(10));
    }
}
