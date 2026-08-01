package com.kinplatform.kin.knowledge;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeCandidateTest {

    @Test
    void of_deberiaConstruirConMetaVacia() {
        var publishedAt = OffsetDateTime.now();
        var candidate = KnowledgeCandidate.of(
            "contenido", "source-1", "Fuente", "https://example.com", publishedAt, "application/json");

        assertEquals("contenido", candidate.content());
        assertEquals("source-1", candidate.sourceId());
        assertEquals("Fuente", candidate.sourceName());
        assertEquals("https://example.com", candidate.url());
        assertEquals(publishedAt, candidate.publishedAt());
        assertEquals("application/json", candidate.contentType());
        assertEquals(Map.of(), candidate.meta());
    }

    @Test
    void constructor_deberiaExponerCampos() {
        var publishedAt = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        var meta = Map.of("lang", "es");
        var candidate = new KnowledgeCandidate(
            "contenido", "source-1", "Fuente", "https://example.com", publishedAt, "text/plain", meta);

        assertEquals(meta, candidate.meta());
        assertEquals(publishedAt, candidate.publishedAt());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var candidate = new KnowledgeCandidate(null, null, null, null, null, null, null);

        assertEquals("", candidate.content());
        assertEquals("", candidate.sourceId());
        assertEquals("", candidate.sourceName());
        assertEquals("", candidate.url());
        assertNull(candidate.publishedAt());
        assertEquals("", candidate.contentType());
        assertEquals(Map.of(), candidate.meta());
    }

    @Test
    void constructor_deberiaProtegerElMeta() {
        var meta = new HashMap<>(Map.of("lang", "es"));
        var candidate = new KnowledgeCandidate("c", "s", "n", "u", null, "t", meta);

        meta.put("otro", "valor");
        assertThrows(UnsupportedOperationException.class,
            () -> candidate.meta().put("extra", "x"));
        assertEquals(1, candidate.meta().size());
    }
}
