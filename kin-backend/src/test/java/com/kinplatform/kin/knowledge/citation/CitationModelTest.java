package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationModelTest {

    private OffsetDateTime published() {
        return OffsetDateTime.parse("2026-01-15T10:00:00-05:00");
    }

    @Test
    void style_deberiaExponerDisplayName() {
        assertEquals("En línea", CitationStyle.INLINE.displayName());
        assertEquals("Nota al pie", CitationStyle.FOOTNOTE.displayName());
        assertEquals("Apéndice", CitationStyle.APPENDIX.displayName());
        assertEquals("Oculto", CitationStyle.HIDDEN.displayName());
        assertEquals("Deshabilitado", CitationStyle.DISABLED.displayName());
    }

    @Test
    void entry_deberiaProtegerNulosYDerivarAnioYConfianza() {
        var entry = new CitationEntry(null, null, null, null, published(), 0.99, null, null, null);

        assertEquals("", entry.sourceId());
        assertEquals("", entry.url());
        assertEquals("", entry.title());
        assertEquals("", entry.license());
        assertEquals("2026", entry.year());
        assertEquals("99", entry.confidencePercent());
    }

    @Test
    void entry_sinFecha_deberiaIndicarSf() {
        var entry = CitationEntry.of("src-1", "https://example.com/x", "MERCADO", null, 0.5, "ok");

        assertEquals("s.f.", entry.year());
        assertEquals(0.5, entry.confidence(), 1e-9);
    }

    @Test
    void decision_deberiaProtegerNulosYExcluir() {
        var decision = new CitationDecision(true, null, null, null, 1.2);

        assertEquals("", decision.sourceId());
        assertEquals("", decision.reason());
        assertEquals(1.0, decision.confidence(), 1e-9);
        var excluded = CitationDecision.excluded("motivo");
        assertTrue(!excluded.included());
        assertEquals("motivo", excluded.reason());
    }

    @Test
    void metadata_deberiaCalcularConteoYConfianzas() {
        var entries = List.of(
            CitationEntry.of("a", "https://example.com/a", "M", published(), 1.0, "ok"),
            CitationEntry.of("b", "https://example.com/b", "E", published(), 0.7, "ok"));

        var metadata = CitationMetadata.of(entries);

        assertEquals(2, metadata.count());
        assertTrue(metadata.sources().contains("a"));
        assertTrue(metadata.sources().contains("b"));
        assertEquals(1.0, metadata.topConfidence(), 1e-9);
        assertEquals(0.85, metadata.averageConfidence(), 1e-9);
    }

    @Test
    void metadata_deberiaDevolverVacioSinEntradas() {
        var metadata = CitationMetadata.of(List.of());

        assertEquals(0, metadata.count());
        assertTrue(metadata.sources().isEmpty());
        assertEquals(0.0, metadata.topConfidence(), 1e-9);
        assertEquals(CitationMetadata.empty(), metadata);
    }

    @Test
    void metadata_deberiaProtegerNulos() {
        var metadata = new CitationMetadata(-1, null, -0.5, 1.5);

        assertEquals(0, metadata.count());
        assertTrue(metadata.sources().isEmpty());
        assertEquals(0.0, metadata.topConfidence(), 1e-9);
        assertEquals(1.0, metadata.averageConfidence(), 1e-9);
    }

    @Test
    void bundle_deberiaProtegerNulos() {
        var bundle = new CitationBundle(null, null, null, null, -1.0, null);

        assertEquals(CitationStyle.INLINE, bundle.style());
        assertTrue(bundle.entries().isEmpty());
        assertTrue(bundle.references().isEmpty());
        assertEquals(CitationMetadata.empty(), bundle.metadata());
        assertEquals(0.0, bundle.score(), 1e-9);
        assertEquals("", bundle.explanation());
        assertTrue(bundle.isEmpty());
    }

    @Test
    void bundle_vacio_conEstilo() {
        var bundle = CitationBundle.empty(CitationStyle.FOOTNOTE, "sin hechos");

        assertEquals(CitationStyle.FOOTNOTE, bundle.style());
        assertTrue(bundle.isEmpty());
        assertEquals("sin hechos", bundle.explanation());
    }

    @Test
    void result_deberiaProtegerNulosYEnvolverBundle() {
        var result = new CitationResult(null, null);

        assertTrue(result.decisions().isEmpty());
        assertTrue(result.isEmpty());
        var empty = CitationResult.empty(CitationStyle.DISABLED, "deshabilitado");
        assertTrue(empty.isEmpty());
        assertEquals(CitationStyle.DISABLED, empty.bundle().style());
    }

    @Test
    void confidence_deberiaMapearConfianzaDeFuente() {
        assertEquals(1.0, CitationConfidence.of(SourceTrust.OFFICIAL_PUBLIC), 1e-9);
        assertEquals(0.7, CitationConfidence.of(SourceTrust.SECONDARY), 1e-9);
        assertEquals(0.4, CitationConfidence.of(SourceTrust.UNVERIFIED), 1e-9);
        assertEquals(0.0, CitationConfidence.of(null), 1e-9);
    }
}
