package com.kinplatform.kin.knowledge.planner;

import java.time.Duration;
import java.util.List;

/**
 * Consulta planificada (especificación Fase 3): apunta únicamente a un
 * {@link ProviderType} abstracto (nunca a un proveedor concreto). La facet es
 * la faceta que la originó; puede ser {@code null} en el caso general de
 * respaldo (consulta web genérica). Valor de dominio inmutable.
 */
public record PlannedQuery(
    String topic,
    List<String> keywords,
    ProviderType providerType,
    IntentFacet facet,
    int limit,
    Duration timeWindow
) {

    public PlannedQuery {
        topic = topic == null ? "" : topic;
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        providerType = providerType == null ? ProviderType.WEB_SEARCH : providerType;
        limit = Math.max(1, Math.min(com.kinplatform.kin.knowledge.KnowledgeRequest.MAX_LIMIT, limit));
        timeWindow = timeWindow == null
            ? com.kinplatform.kin.knowledge.KnowledgeRequest.DEFAULT_TIME_WINDOW
            : timeWindow;
    }

    public static PlannedQuery of(String topic, List<String> keywords, ProviderType providerType,
                                  IntentFacet facet, int limit, Duration timeWindow) {
        return new PlannedQuery(topic, keywords, providerType, facet, limit, timeWindow);
    }
}
