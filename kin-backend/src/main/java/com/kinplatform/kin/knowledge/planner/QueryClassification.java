package com.kinplatform.kin.knowledge.planner;

import java.util.Set;

/**
 * Clasificación de la consulta (especificación Fase 3): tipo primario, dominio
 * principal, dominios implicados (para decidir hibridez), facetas y prioridad
 * determinista. Valor de dominio inmutable.
 */
public record QueryClassification(
    IntentType type,
    QueryDomain domain,
    Set<QueryDomain> domains,
    Set<IntentFacet> facets,
    int priority
) {

    public QueryClassification {
        type = type == null ? IntentType.GENERAL : type;
        domain = domain == null ? QueryDomain.GENERAL : domain;
        domains = domains == null || domains.isEmpty()
            ? Set.of(domain)
            : Set.copyOf(domains);
        facets = facets == null ? Set.of() : Set.copyOf(facets);
        priority = Math.max(0, priority);
    }

    public static QueryClassification general() {
        return new QueryClassification(IntentType.GENERAL, QueryDomain.GENERAL,
            Set.of(QueryDomain.GENERAL), Set.of(), 1);
    }
}
