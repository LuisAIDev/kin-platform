package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Provider First: prioriza proveedores frescos; no prefiere caché.
 */
public final class ProviderFirstPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.PROVIDER_FIRST;
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
