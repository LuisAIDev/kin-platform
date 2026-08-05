package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Internet First: prioriza fuentes frescas por Internet; degrada.
 */
public final class InternetFirstPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.INTERNET_FIRST;
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
