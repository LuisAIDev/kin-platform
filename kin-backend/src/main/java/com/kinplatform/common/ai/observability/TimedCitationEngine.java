package com.kinplatform.common.ai.observability;

import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.citation.CitationEngine;
import com.kinplatform.common.knowledge.citation.CitationPolicy;
import com.kinplatform.common.knowledge.citation.CitationResult;
import com.kinplatform.common.knowledge.citation.CitationStyle;

/**
 * Decorador observador del {@link CitationEngine} (Fase 7 — observabilidad).
 * Mide la latencia de citación y registra estilo, entradas y bundles. No altera
 * el {@link CitationResult} producido.
 */
public class TimedCitationEngine extends CitationEngine {

    private final KnowledgeMetrics metrics;

    public TimedCitationEngine(KnowledgeMetrics metrics) {
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public CitationResult produce(KnowledgeResult result, CitationPolicy policy, CitationStyle style) {
        long start = System.nanoTime();
        CitationResult citation = super.produce(result, policy, style);
        metrics.stage("citation", TimedQueryPlanner.toMs(start));
        metrics.citationBundles();
        metrics.citationStyle(citation.bundle().style().name());
        metrics.citationEntries(citation.bundle().entries().size());
        return citation;
    }
}


