package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Estrategia Local First: prioriza el conocimiento local (documentos/RAG).
 */
public final class LocalFirstPolicy implements OrchestrationStrategyPolicy {

    @Override
    public OrchestrationStrategy strategy() {
        return OrchestrationStrategy.LOCAL_FIRST;
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
