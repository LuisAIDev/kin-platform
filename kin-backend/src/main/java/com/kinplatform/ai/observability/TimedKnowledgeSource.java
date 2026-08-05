package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.List;

/**
 * Decorador observador de una {@link KnowledgeSource} (Fase 7 — observabilidad).
 * Mide consultas, latencia, errores y timeouts del fetch por {@link ProviderType}.
 */
public class TimedKnowledgeSource implements KnowledgeSource {

    private final ProviderType providerType;
    private final KnowledgeSource delegate;
    private final KnowledgeMetrics metrics;
    private final long timeoutMs;

    public TimedKnowledgeSource(ProviderType providerType, KnowledgeSource delegate, KnowledgeMetrics metrics) {
        this(providerType, delegate, metrics, 30_000L);
    }

    public TimedKnowledgeSource(ProviderType providerType, KnowledgeSource delegate,
                                KnowledgeMetrics metrics, long timeoutMs) {
        this.providerType = providerType;
        this.delegate = delegate;
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
        this.timeoutMs = Math.max(0, timeoutMs);
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        String type = providerType == null ? "UNKNOWN" : providerType.name();
        metrics.providerRequest(type);
        long start = System.nanoTime();
        try {
            List<KnowledgeCandidate> result = delegate.fetch(query);
            long tookMs = TimedQueryPlanner.toMs(start);
            metrics.providerLatency(type, tookMs);
            if (tookMs >= timeoutMs) {
                metrics.providerTimeout(type);
            }
            return result;
        } catch (RuntimeException ex) {
            metrics.providerError(type);
            throw ex;
        }
    }
}
