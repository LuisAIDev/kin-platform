package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la cadena de integración: {@code KnowledgeResult → CitationEngine →
 * CitationBundle → consumidor de prompt}. El consumidor (futuro
 * PromptContextBuilder, frontera ADR-012) consume únicamente el
 * {@link CitationBundle}, nunca {@link KnowledgeFact} ni metadatos crudos.
 */
class CitationIntegrationTest {

    private final CitationEngine engine = new CitationEngine();

    private KnowledgeResult result() {
        var facts = List.of(
            KnowledgeFact.of("El mercado retail creció 12% anual.", "src-1",
                "https://example.com/reporte", OffsetDateTime.parse("2026-01-15T10:00:00-05:00"),
                SourceTrust.OFFICIAL_PUBLIC, "MERCADO"),
            KnowledgeFact.of("Las PYMEs digitalizan sus ventas.", "src-2",
                "https://example.com/estudio", OffsetDateTime.parse("2026-02-01T10:00:00-05:00"),
                SourceTrust.SECONDARY, "MERCADO"));
        return new KnowledgeResult(facts, List.of("src-1", "src-2"), List.of(), 0.85,
            "ok", "KnowledgeEngine", "v1");
    }

    @Test
    void knowledgeResult_aCitationEngine_deberiaProducirBundle() {
        var citation = engine.produce(result(), null, CitationStyle.FOOTNOTE);

        assertFalse(citation.isEmpty());
        assertEquals(2, citation.bundle().entries().size());
        assertEquals(2, citation.bundle().references().size());
    }

    @Test
    void bundle_deberiaSerAutocontenidoParaElConsumidor() {
        var citation = engine.produce(result(), null, CitationStyle.APPENDIX);
        var consumer = new PromptCitationConsumer();

        var snippet = consumer.render(citation.bundle());

        assertTrue(snippet.contains("[1]"));
        assertTrue(snippet.contains("[2]"));
        assertTrue(snippet.contains("https://example.com/reporte"));
        assertFalse(snippet.contains("El mercado retail creció")); // no se filtra el hecho crudo
    }

    @Test
    void consumer_noConoceMetadatosCrudos_soloElBundle() {
        var citation = engine.produce(result(), null, CitationStyle.INLINE);

        assertTrue(citation.bundle().references().stream().allMatch(ref -> ref.startsWith("(")));
        assertEquals(CitationStyle.INLINE, citation.bundle().style());
    }

    /**
     * Consumidor mínimo que representa la futura integración con el
     * PromptContextBuilder (frontera ADR-012): recibe solo el {@link CitationBundle}
     * y produce texto de referencias. No conoce {@link KnowledgeFact} ni
     * SourceMetadata.
     */
    private static final class PromptCitationConsumer {
        String render(CitationBundle bundle) {
            return String.join("\n", bundle.references());
        }
    }
}
