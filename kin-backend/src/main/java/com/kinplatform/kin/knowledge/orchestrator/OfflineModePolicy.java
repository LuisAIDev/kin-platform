package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Offline Mode: restringe el ciclo a conocimiento local/caché, sin
 * Internet; degrada ante consultas externas.
 */
public final class OfflineModePolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.OFFLINE_MODE;
    }

    @Override
    public boolean prefersCache() {
        return false;
    }

    @Override
    public boolean offlineOnly() {
        return true;
    }

    @Override
    public boolean failureIsFatal() {
        return false;
    }
}
