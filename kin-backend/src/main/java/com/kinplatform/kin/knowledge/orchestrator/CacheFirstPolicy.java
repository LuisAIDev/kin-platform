package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Cache First: prefiere reutilizar la caché; ante fallos degrada.
 */
public final class CacheFirstPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.CACHE_FIRST;
    }

    @Override
    public boolean prefersCache() {
        return true;
    }

    @Override
    public boolean offlineOnly() {
        return false;
    }

    @Override
    public boolean failureIsFatal() {
        return false;
    }
}
