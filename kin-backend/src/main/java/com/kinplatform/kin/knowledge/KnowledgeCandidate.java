package com.kinplatform.kin.knowledge;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Resultado crudo devuelto por una fuente de conocimiento (ADR-014).
 *
 * <p>El dominio no ve la red: un adaptador de infraestructura entrega
 * candidatos y Java decide (con {@code SourceValidator} en etapas posteriores)
 * cuáles se convierten en hechos verificados.</p>
 */
public record KnowledgeCandidate(
    String content,
    String sourceId,
    String sourceName,
    String url,
    OffsetDateTime publishedAt,
    String contentType,
    Map<String, String> meta
) {

    public KnowledgeCandidate {
        content = content == null ? "" : content;
        sourceId = sourceId == null ? "" : sourceId;
        sourceName = sourceName == null ? "" : sourceName;
        url = url == null ? "" : url;
        contentType = contentType == null ? "" : contentType;
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static KnowledgeCandidate of(
        String content,
        String sourceId,
        String sourceName,
        String url,
        OffsetDateTime publishedAt,
        String contentType
    ) {
        return new KnowledgeCandidate(content, sourceId, sourceName, url, publishedAt, contentType, Map.of());
    }
}
