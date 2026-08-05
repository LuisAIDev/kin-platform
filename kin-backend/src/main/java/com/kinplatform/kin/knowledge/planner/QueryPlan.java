package com.kinplatform.kin.knowledge.planner;

import java.util.List;

/**
 * Plan de consulta (especificación Fase 3): clasificación, estrategia elegida,
 * consultas planificadas y motivos deterministas. Inmutable; el plan nunca
 * ejecuta nada — solo describe lo que el orquestador (Fase 5) podrá ejecutar.
 */
public record QueryPlan(
    QueryClassification classification,
    QueryStrategy strategy,
    List<PlannedQuery> queries,
    List<String> reasons
) {

    public QueryPlan {
        classification = classification == null ? QueryClassification.general() : classification;
        strategy = strategy == null ? QueryStrategy.SINGLE : strategy;
        queries = queries == null ? List.of() : List.copyOf(queries);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public boolean isEmpty() {
        return queries.isEmpty();
    }

    public static QueryPlan empty() {
        return new QueryPlan(QueryClassification.general(), QueryStrategy.SINGLE,
            List.of(), List.of("Sin consultas planificadas"));
    }
}
