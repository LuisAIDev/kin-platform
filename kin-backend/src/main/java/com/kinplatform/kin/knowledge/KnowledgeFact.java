package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.engine.DeterministicId;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hecho de conocimiento verificado y normalizado (ADR-014).
 *
 * <p>Inmutable, con ID determinista derivado de su contenido
 * ({@link DeterministicId}) para trazabilidad reproducible sin estado: el mismo
 * dato produce siempre el mismo id.</p>
 */
public record KnowledgeFact(
    UUID id,
    String claim,
    String sourceId,
    String url,
    OffsetDateTime publishedAt,
    SourceTrust trust,
    String category
) {

    public KnowledgeFact {
        claim = claim == null ? "" : claim;
        sourceId = sourceId == null ? "" : sourceId;
        url = url == null ? "" : url;
        category = category == null ? "" : category;
        trust = trust == null ? SourceTrust.UNVERIFIED : trust;
        id = id == null ? DeterministicId.from(category, claim, sourceId) : id;
    }

    public static KnowledgeFact of(
        String claim,
        String sourceId,
        String url,
        OffsetDateTime publishedAt,
        SourceTrust trust,
        String category
    ) {
        return new KnowledgeFact(null, claim, sourceId, url, publishedAt, trust, category);
    }
}
