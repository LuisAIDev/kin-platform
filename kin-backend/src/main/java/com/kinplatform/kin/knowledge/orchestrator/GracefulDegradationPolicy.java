package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Graceful Degradation: degradación controlada ante fallos.
 */
public final class GracefulDegradationPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.GRACEFUL_DEGRADATION;
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
