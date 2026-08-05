package com.kinplatform.kin.knowledge.planner;

import java.util.Set;

/**
 * Intención detectada por el {@code IntentAnalyzer} (especificación Fase 3):
 * tipo primario, conjunto de facetas y tema. Valor de dominio inmutable.
 */
public record QueryIntent(
    IntentType type,
    Set<IntentFacet> facets,
    String topic
) {

    public QueryIntent {
        type = type == null ? IntentType.GENERAL : type;
        facets = facets == null ? Set.of() : Set.copyOf(facets);
        topic = topic == null ? "" : topic;
    }

    public static QueryIntent general() {
        return new QueryIntent(IntentType.GENERAL, Set.of(), "");
    }
}
