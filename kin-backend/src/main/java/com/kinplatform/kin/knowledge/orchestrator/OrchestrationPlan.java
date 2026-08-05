package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.policy.QueryMode;
import com.kinplatform.kin.knowledge.planner.PlannedQuery;
import com.kinplatform.kin.knowledge.planner.QueryPlan;

import java.util.List;

/**
 * Plan de orquestación inmutable (especificación Fase 5): estrategia activa,
 * modo de consulta decidido por el Policy Engine y el {@link QueryPlan} del
 * planner. El orquestador nunca ejecuta el plan: solo lo coordina.
 */
public record OrchestrationPlan(
    OrchestrationStrategy strategy,
    QueryMode mode,
    QueryPlan queryPlan
) {

    public OrchestrationPlan {
        strategy = strategy == null ? OrchestrationStrategy.GRACEFUL_DEGRADATION : strategy;
        mode = mode == null ? QueryMode.EXTERNAL : mode;
        queryPlan = queryPlan == null ? QueryPlan.empty() : queryPlan;
    }

    public List<PlannedQuery> queries() {
        return queryPlan.queries();
    }

    public boolean isEmpty() {
        return queryPlan.isEmpty();
    }
}
