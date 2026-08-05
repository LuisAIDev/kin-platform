package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategias de orquestación (especificación Fase 5): modeladas como
 * estrategias registrables ({@link OrchestrationStrategyPolicy}), nunca como
 * condiciones gigantes. Definen preferencia de caché, restricción offline y
 * severidad ante fallos.
 */
public enum OrchestrationStrategy {

    CACHE_FIRST("Cache first"),
    PROVIDER_FIRST("Provider first"),
    HYBRID("Híbrido"),
    LOCAL_FIRST("Local first"),
    INTERNET_FIRST("Internet first"),
    FAIL_FAST("Fail fast"),
    GRACEFUL_DEGRADATION("Degradación controlada"),
    OFFLINE_MODE("Modo offline");

    private final String displayName;

    OrchestrationStrategy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
