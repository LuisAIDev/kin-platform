package com.kinplatform.kin.knowledge;

import java.time.Duration;
import java.util.List;

/**
 * Consulta proyectada para una fuente concreta (ADR-014): subconjunto del
 * {@link KnowledgeRequest} que entiende el puerto {@link KnowledgeSource}.
 *
 * <p>Java la deriva en el gateway de conocimiento; la fuente nunca ve el
 * request completo, solo lo que necesita para buscar.</p>
 */
public record KnowledgeQuery(
    String topic,
    List<String> keywords,
    int limit,
    Duration timeWindow
) {

    public KnowledgeQuery {
        topic = topic == null ? "" : topic;
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        limit = Math.max(1, Math.min(KnowledgeRequest.MAX_LIMIT, limit));
        timeWindow = timeWindow == null ? KnowledgeRequest.DEFAULT_TIME_WINDOW : timeWindow;
    }

    public static KnowledgeQuery from(KnowledgeRequest request) {
        return new KnowledgeQuery(
            request.topic(), request.keywords(), request.limit(), request.timeWindow());
    }
}
