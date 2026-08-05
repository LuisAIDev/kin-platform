package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.orchestrator.ContextRanker;
import com.kinplatform.kin.knowledge.orchestrator.RankedCandidate;

import java.util.List;

/**
 * Decorador observador del {@link ContextRanker} (Fase 7 — observabilidad).
 * Mide la latencia del ranking. No altera el orden producido por el delegado.
 */
public class TimedContextRanker implements ContextRanker {

    private final ContextRanker delegate;
    private final KnowledgeMetrics metrics;

    public TimedContextRanker(ContextRanker delegate, KnowledgeMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public List<RankedCandidate> rank(List<RankedCandidate> candidates) {
        long start = System.nanoTime();
        List<RankedCandidate> result = delegate.rank(candidates);
        metrics.stage("ranking", TimedQueryPlanner.toMs(start));
        return result;
    }
}
