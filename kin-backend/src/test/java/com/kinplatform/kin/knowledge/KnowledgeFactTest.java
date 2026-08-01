package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.engine.DeterministicId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeFactTest {

    @Test
    void of_deberiaDerivarIdDeterminista() {
        var publishedAt = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        var fact = KnowledgeFact.of(
            "El mercado crece un 12% anual", "source-1", "https://example.com",
            publishedAt, SourceTrust.OFFICIAL_PUBLIC, "MERCADO");

        assertNotNull(fact.id());
        assertEquals(DeterministicId.from("MERCADO", "El mercado crece un 12% anual", "source-1"), fact.id());
        assertEquals("El mercado crece un 12% anual", fact.claim());
        assertEquals("source-1", fact.sourceId());
        assertEquals("https://example.com", fact.url());
        assertEquals(publishedAt, fact.publishedAt());
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, fact.trust());
        assertEquals("MERCADO", fact.category());
    }

    @Test
    void mismoContenido_deberiaProducirMismoId() {
        var a = KnowledgeFact.of("claim", "s1", "u", null, SourceTrust.SECONDARY, "C");
        var b = KnowledgeFact.of("claim", "s1", "u", null, SourceTrust.SECONDARY, "C");

        assertEquals(a.id(), b.id());
    }

    @Test
    void contenidoDistinto_deberiaProducirIdDistinto() {
        var a = KnowledgeFact.of("claim A", "s1", "u", null, SourceTrust.SECONDARY, "C");
        var b = KnowledgeFact.of("claim B", "s1", "u", null, SourceTrust.SECONDARY, "C");

        assertNotEquals(a.id(), b.id());
    }

    @Test
    void constructor_deberiaConservarElIdProvisto() {
        var id = UUID.randomUUID();
        var fact = new KnowledgeFact(id, "claim", "s1", "u", null, SourceTrust.UNVERIFIED, "C");

        assertEquals(id, fact.id());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var fact = new KnowledgeFact(null, null, null, null, null, null, null);

        assertNotNull(fact.id());
        assertEquals("", fact.claim());
        assertEquals("", fact.sourceId());
        assertEquals("", fact.url());
        assertEquals(SourceTrust.UNVERIFIED, fact.trust());
        assertEquals("", fact.category());
    }
}
