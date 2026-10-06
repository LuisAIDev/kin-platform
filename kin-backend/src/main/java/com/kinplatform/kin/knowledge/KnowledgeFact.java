package com.kinplatform.kin.knowledge;

import com.kinplatform.common.engine.DeterministicId;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hecho de conocimiento verificado y normalizado (ADR-014).
 *
 * <p>Inmutable, con ID determinista derivado de su contenido
 * ({@link DeterministicId}) para trazabilidad reproducible sin estado: el mismo
 * dato produce siempre el mismo id. {@code maxAge} conserva la ventana de
 * frescura de la fuente (ADR-025) para el TTL de caché por fuente.</p>
 */
public record KnowledgeFact(
        UUID id,
        String claim,
        String sourceId,
        String url,
        OffsetDateTime publishedAt,
        SourceTrust trust,
        String category,
        Duration maxAge) {

    public KnowledgeFact(
            UUID id,
            String claim,
            String sourceId,
            String url,
            OffsetDateTime publishedAt,
            SourceTrust trust,
            String category) {
        this(id, claim, sourceId, url, publishedAt, trust, category, null);
    }

    public KnowledgeFact {
        claim = claim == null ? "" : claim;
        sourceId = sourceId == null ? "" : sourceId;
        url = url == null ? "" : url;
        category = category == null ? "" : category;
        trust = trust == null ? SourceTrust.UNVERIFIED : trust;
        id = id == null ? DeterministicId.from(category, claim, sourceId) : id;
    }

    public static KnowledgeFact of(
            String claim, String sourceId, String url, OffsetDateTime publishedAt, SourceTrust trust, String category) {
        return new KnowledgeFact(null, claim, sourceId, url, publishedAt, trust, category, null);
    }
}

