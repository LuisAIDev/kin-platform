package com.kinplatform.kin.knowledge;

import java.time.Duration;
import java.util.List;

/**
 * Consulta proyectada para una fuente concreta (ADR-014): subconjunto del
 * {@link KnowledgeRequest} que entiende el puerto {@link KnowledgeSource}.
 *
 * <p>Java la deriva en el gateway de conocimiento; la fuente nunca ve el
 * request completo, solo lo que necesita para buscar. {@code category}
 * transporta la categoría del proyecto para que los adaptadores (p. ej.
 * {@code CategoryAwareCompositeKnowledgeSource}) filtren las fuentes
 * pertinentes antes de consultar.</p>
 */
public record KnowledgeQuery(String topic, List<String> keywords, int limit, Duration timeWindow, String category) {

    public KnowledgeQuery {
        topic = topic == null ? "" : topic;
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        limit = Math.max(1, Math.min(KnowledgeRequest.MAX_LIMIT, limit));
        timeWindow = timeWindow == null ? KnowledgeRequest.DEFAULT_TIME_WINDOW : timeWindow;
        category = category == null ? "" : category;
    }

    /** Constructor de conveniencia sin categoría (compatibilidad). */
    public KnowledgeQuery(String topic, List<String> keywords, int limit, Duration timeWindow) {
        this(topic, keywords, limit, timeWindow, "");
    }

    public static KnowledgeQuery from(KnowledgeRequest request) {
        return new KnowledgeQuery(
                request.topic(), request.keywords(), request.limit(), request.timeWindow(), request.category());
    }
}
