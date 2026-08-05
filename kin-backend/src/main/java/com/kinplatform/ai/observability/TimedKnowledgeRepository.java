package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeResult;

import java.time.Duration;
import java.util.Optional;

/**
 * Decorador observador del {@link KnowledgeRepository} (Fase 7 — observabilidad).
 * Registra hits, misses, consultas evitadas y guardados. Un hit evita una
 * consulta externa (consulta evitada). No altera el comportamiento del caché.
 */
public class TimedKnowledgeRepository implements KnowledgeRepository {

    private final KnowledgeRepository delegate;
    private final KnowledgeMetrics metrics;

    public TimedKnowledgeRepository(KnowledgeRepository delegate, KnowledgeMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public Optional<KnowledgeResult> find(KnowledgeQuery query) {
        long start = System.nanoTime();
        metrics.stage("cache", TimedQueryPlanner.toMs(start));
        if (delegate == null) {
            metrics.cacheMiss();
            return Optional.empty();
        }
        Optional<KnowledgeResult> result = delegate.find(query);
        if (result.isPresent()) {
            metrics.cacheHit();
            metrics.cacheAvoidedQuery();
        } else {
            metrics.cacheMiss();
        }
        return result;
    }

    @Override
    public void save(KnowledgeResult result, Duration ttl) {
        if (delegate != null) {
            delegate.save(result, ttl);
            metrics.cacheSaved();
        }
    }
}
