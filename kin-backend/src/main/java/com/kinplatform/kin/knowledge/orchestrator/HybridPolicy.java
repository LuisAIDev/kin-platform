package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Hybrid: combina caché, proveedores y conocimiento local; degrada.
 */
public final class HybridPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.HYBRID;
    }

    @Override
    public boolean prefersCache() {
        return false;
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
