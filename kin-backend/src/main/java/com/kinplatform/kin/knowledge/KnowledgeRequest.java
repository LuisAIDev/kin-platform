package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.context.AnalyzedDimension;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Solicitud de adquisición de conocimiento externo (ADR-014): tema a
 * enriquecer, dimensiones objetivo, palabras clave, límite de resultados y
 * ventana temporal.
 *
 * <p>Java la construye antes de consultar ninguna fuente. {@code dimensions}
 * limita el alcance a las dimensiones del {@link AnalyzedDimension} que el
 * turno necesita enriquecer; {@code timeWindow} acota la frescura aceptada.</p>
 */
public record KnowledgeRequest(
    String topic,
    Set<AnalyzedDimension> dimensions,
    List<String> keywords,
    int limit,
    Duration timeWindow
) {

    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 20;
    public static final Duration DEFAULT_TIME_WINDOW = Duration.ofDays(365);

    public KnowledgeRequest {
        topic = topic == null ? "" : topic;
        dimensions = dimensions == null ? Set.of() : Set.copyOf(dimensions);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        limit = Math.max(1, Math.min(MAX_LIMIT, limit));
        timeWindow = timeWindow == null ? DEFAULT_TIME_WINDOW : timeWindow;
    }

    public static KnowledgeRequest of(String topic, List<String> keywords) {
        return new KnowledgeRequest(topic, Set.of(), keywords, DEFAULT_LIMIT, DEFAULT_TIME_WINDOW);
    }

    public static KnowledgeRequest empty() {
        return new KnowledgeRequest("", Set.of(), List.of(), DEFAULT_LIMIT, DEFAULT_TIME_WINDOW);
    }
}
