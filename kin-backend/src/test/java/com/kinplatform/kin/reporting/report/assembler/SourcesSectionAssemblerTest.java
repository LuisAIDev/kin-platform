package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.EvidenceScore;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourcesSectionAssemblerTest {

    private final SourcesSectionAssembler assembler = new SourcesSectionAssembler();

    @Test
    void sinEnriquecimiento_deberiaDevolverVacia() {
        var section = assembler.assemble(TestReportInputs.input());
        assertTrue(section.isEmpty());
    }

    @Test
    void conEnriquecimientoVacio_deberiaDevolverVacia() {
        var section = assembler.assemble(TestReportInputs.input().withEnrichment(EnrichmentResult.empty()));
        assertTrue(section.isEmpty());
    }

    @Test
    void conEvidencia_deberiaConstruirFuentesCitadas() {
        var enrichment = new EnrichmentResult(List.of(
            rank(EvidenceCategory.MARKET, "El mercado crece 15%", "src-market", "https://x/market", 0.8),
            rank(EvidenceCategory.FINANCIAL, "Margen 20%", "src-fin", "https://x/fin", 0.7)),
            List.of("src-market", "src-fin"), 0.75, "enriquecido", "Test", "v1");

        var section = assembler.assemble(TestReportInputs.input().withEnrichment(enrichment));

        assertEquals(2, section.sources().size());
        var market = section.sources().get(0);
        assertEquals("src-market", market.sourceId());
        assertEquals("https://x/market", market.url());
        assertEquals("El mercado crece 15%", market.claim());
        assertEquals(EvidenceCategory.MARKET, market.category());
        assertEquals(0.8, market.score());
    }

    @Test
    void mismaFuente_deberiaDeduplicarConservandoMayorScore() {
        var fact = KnowledgeFact.of("claim", "src-1", "https://x", OffsetDateTime.now(),
            SourceTrust.OFFICIAL_PUBLIC, "sector");
        var rank = EvidenceRank.of(EvidenceCategory.MARKET, List.of(
            new KnowledgeEvidence(fact, EvidenceScore.of(0.5, EvidenceCategory.MARKET, "a")),
            new KnowledgeEvidence(fact, EvidenceScore.of(0.9, EvidenceCategory.MARKET, "b"))));
        var enrichment = new EnrichmentResult(List.of(rank), List.of("src-1"), 0.9,
            "enriquecido", "Test", "v1");

        var section = assembler.assemble(TestReportInputs.input().withEnrichment(enrichment));

        assertEquals(1, section.sources().size());
        assertEquals("src-1", section.sources().get(0).sourceId());
        assertEquals(0.9, section.sources().get(0).score());
    }

    @Test
    void hechoSinSourceId_deberiaIgnorarse() {
        var fact = KnowledgeFact.of("claim", null, "https://x", OffsetDateTime.now(),
            SourceTrust.OFFICIAL_PUBLIC, "sector");
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(new KnowledgeEvidence(fact, EvidenceScore.of(0.8, EvidenceCategory.MARKET, "ok"))));
        var enrichment = new EnrichmentResult(List.of(rank), List.of(), 0.8,
            "enriquecido", "Test", "v1");

        var section = assembler.assemble(TestReportInputs.input().withEnrichment(enrichment));

        assertTrue(section.isEmpty());
    }

    private static EvidenceRank rank(EvidenceCategory category, String claim, String sourceId,
                                     String url, double score) {
        var fact = KnowledgeFact.of(claim, sourceId, url, OffsetDateTime.now(),
            SourceTrust.OFFICIAL_PUBLIC, "sector");
        return EvidenceRank.of(category,
            List.of(new KnowledgeEvidence(fact, EvidenceScore.of(score, category, "Relevante."))));
    }
}
