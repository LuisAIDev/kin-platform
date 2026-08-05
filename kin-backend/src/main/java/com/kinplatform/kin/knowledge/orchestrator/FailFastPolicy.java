package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Fail Fast: cualquier fallo detiene el ciclo de inmediato.
 */
public final class FailFastPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.FAIL_FAST;
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
        return true;
    }
}
