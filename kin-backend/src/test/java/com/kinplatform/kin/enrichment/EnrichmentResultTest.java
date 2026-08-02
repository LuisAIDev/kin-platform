package com.kinplatform.kin.enrichment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrichmentResultTest {

    private KnowledgeEvidence evidence(double value, String sourceId) {
        var fact = com.kinplatform.kin.knowledge.KnowledgeFact.of("Hecho " + value, sourceId,
            "https://example.com", null, null, "mercado");
        return new KnowledgeEvidence(fact, EvidenceScore.of(value, EvidenceCategory.MARKET, ""));
    }

    @Test
    void deberiaNormalizarListasNulas() {
        var result = new EnrichmentResult(null, null, 0.5, null, null, null);

        assertTrue(result.ranks().isEmpty());
        assertTrue(result.sourcesUsed().isEmpty());
        assertEquals("", result.explanation());
        assertEquals("", result.generatedBy());
        assertEquals("", result.engineVersion());
    }

    @Test
    void empty_deberiaProducirResultadoSinEvidencia() {
        var result = EnrichmentResult.empty();

        assertTrue(result.isEmpty());
        assertEquals(0, result.totalEvidence());
        assertEquals(0, result.rankCount());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void deberiaAgregarEvidenciaYCategorias() {
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(evidence(0.9, "src-1"), evidence(0.7, "src-2")));
        var result = new EnrichmentResult(List.of(rank), List.of("src-1", "src-2"), 0.8, "", "E", "v1");

        assertEquals(2, result.totalEvidence());
        assertEquals(1, result.rankCount());
        assertFalse(result.isEmpty());
        assertTrue(result.rankFor(EvidenceCategory.MARKET).isPresent());
        assertFalse(result.rankFor(EvidenceCategory.INNOVATION).isPresent());
    }

    @Test
    void isEmpty_deberiaConsiderarLaEvidenciaTotal() {
        var vacio = new EnrichmentResult(List.of(), List.of(), 0.0, "", "E", "v1");
        assertTrue(vacio.isEmpty());

        var conRankVacio = new EnrichmentResult(
            List.of(EvidenceRank.of(EvidenceCategory.MARKET, List.of())), List.of(), 0.0, "", "E", "v1");
        assertTrue(conRankVacio.isEmpty());
    }

    @Test
    void deberiaAcotarConfianza() {
        assertEquals(1.0, new EnrichmentResult(List.of(), List.of(), 2.0, "", "", "").confidence(), 1e-9);
        assertEquals(0.0, new EnrichmentResult(List.of(), List.of(), -1.0, "", "", "").confidence(), 1e-9);
    }

    @Test
    void deberiaExponerRankingSolicitado() {
        var rank = EvidenceRank.of(EvidenceCategory.FINANCIAL, List.of(evidence(0.6, "src-1")));
        var result = new EnrichmentResult(List.of(rank), List.of(), 0.6, "", "", "");

        assertTrue(result.rankFor(EvidenceCategory.FINANCIAL).isPresent());
        assertEquals(0.6, result.rankFor(EvidenceCategory.FINANCIAL).get().confidence(), 1e-9);
    }
}
