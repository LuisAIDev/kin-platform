package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.orchestrator.ContextAssembler;
import com.kinplatform.kin.knowledge.orchestrator.RankedCandidate;

import java.util.List;

/**
 * Decorador observador del {@link ContextAssembler} (Fase 7 — observabilidad).
 * Mide la latencia del ensamblado y registra fuentes aceptadas, puntaje y
 * confianza media. No altera el {@link KnowledgeResult} producido.
 */
public class TimedContextAssembler implements ContextAssembler {

    private final ContextAssembler delegate;
    private final KnowledgeMetrics metrics;

    public TimedContextAssembler(ContextAssembler delegate, KnowledgeMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public KnowledgeResult assemble(KnowledgeQuery query, List<RankedCandidate> ranked) {
        long start = System.nanoTime();
        KnowledgeResult result = delegate.assemble(query, ranked);
        metrics.stage("assembler", TimedQueryPlanner.toMs(start));
        return result;
    }

    @Override
    public KnowledgeResult emptyResult(String reason) {
        return delegate.emptyResult(reason);
    }
}
