package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.planner.QueryPlan;
import com.kinplatform.kin.knowledge.planner.QueryPlanner;

/**
 * Decorador observador del {@link QueryPlanner} (Fase 7 — observabilidad).
 * Mide la latencia del planner y registra intención, estrategias, facetas y
 * consultas. No altera el {@link QueryPlan} producido (mismas entradas →
 * mismas salidas).
 */
public class TimedQueryPlanner extends QueryPlanner {

    private final KnowledgeMetrics metrics;

    public TimedQueryPlanner(KnowledgeMetrics metrics) {
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public QueryPlan plan(KnowledgeRequest request) {
        long start = System.nanoTime();
        QueryPlan plan = super.plan(request);
        long tookMs = toMs(start);
        metrics.stage("planner", tookMs);
        metrics.plannerIntent(plan.classification().type().name());
        metrics.plannerStrategy(plan.strategy().name());
        metrics.plannerQueryStrategy(plan.strategy().name());
        metrics.plannerFacets(plan.classification().facets().size());
        metrics.plannerQueries(plan.queries().size());
        return plan;
    }

    static long toMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
