package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationCollectorTest {

    private KnowledgeFact fact(String sourceId, String url) {
        return KnowledgeFact.of("Dato verificado.", sourceId, url,
            OffsetDateTime.now().minusDays(3), SourceTrust.OFFICIAL_PUBLIC, "MERCADO");
    }

    private CitationDecision include(String sourceId, String url) {
        return new CitationDecision(true, sourceId, url, "ok", 1.0);
    }

    @Test
    void soloIncluidas_deberiaRecolectarse() {
        var facts = List.of(fact("a", "https://example.com/a"), fact("b", "https://example.com/b"));
        var decisions = List.of(include("a", "https://example.com/a"), CitationDecision.excluded("no"));

        var entries = new CitationCollector().collect(facts, decisions);

        assertEquals(1, entries.size());
        assertEquals("a", entries.get(0).sourceId());
        assertEquals("MERCADO", entries.get(0).sourceType());
        assertEquals(1.0, entries.get(0).confidence(), 1e-9);
    }

    @Test
    void duplicadas_deberianDeduplicarsePorFuenteYUrl() {
        var facts = List.of(
            fact("a", "https://example.com/a"),
            fact("a", "https://example.com/a"),
            fact("a", "https://example.com/b"));
        var decisions = List.of(include("a", "https://example.com/a"),
            include("a", "https://example.com/a"), include("a", "https://example.com/b"));

        var entries = new CitationCollector().collect(facts, decisions);

        assertEquals(2, entries.size());
    }

    @Test
    void entradasNulasOCandidatoNulo_deberianManejarse() {
        var collector = new CitationCollector();

        assertTrue(collector.collect(null, List.of()).isEmpty());
        assertTrue(collector.collect(List.of(), null).isEmpty());
        assertTrue(collector.collect(List.of(fact("a", "https://x")), List.of()).isEmpty());
        var withNull = collector.collect(
            Arrays.asList(fact("a", "https://example.com/a"), null),
            List.of(include("a", "https://example.com/a"), include("b", "https://example.com/b")));
        assertEquals(1, withNull.size());
    }

    @Test
    void decisionesSobrantes_deberianIgnorarse() {
        var facts = List.of(fact("a", "https://example.com/a"));
        var decisions = List.of(include("a", "https://example.com/a"), include("b", "https://example.com/b"));

        var entries = new CitationCollector().collect(facts, decisions);

        assertEquals(1, entries.size());
    }
}
