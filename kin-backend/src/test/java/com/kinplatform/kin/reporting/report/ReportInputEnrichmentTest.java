package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.EvidenceScore;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportInputEnrichmentTest {

    @Test
    void constructor10Parametros_deberiaNormalizarEnrichmentVacio() {
        var input = TestReportInputs.input();
        assertNotNull(input.enrichment());
        assertTrue(input.enrichment().isEmpty());
    }

    @Test
    void constructor11Parametros_deberiaConservarEnrichment() {
        var enrichment = someEnrichment();
        var base = TestReportInputs.input();
        var input = new ReportInput(base.projectId(), base.projectTitle(), base.projectCategory(),
            base.projectContext(), base.evaluation(), base.decision(), base.score(),
            base.recommendation(), base.risk(), base.opportunity(), enrichment);

        assertEquals(enrichment, input.enrichment());
    }

    @Test
    void constructor11Parametros_conNull_deberiaNormalizarVacio() {
        var base = TestReportInputs.input();
        var input = new ReportInput(base.projectId(), base.projectTitle(), base.projectCategory(),
            base.projectContext(), base.evaluation(), base.decision(), base.score(),
            base.recommendation(), base.risk(), base.opportunity(), null);

        assertNotNull(input.enrichment());
        assertTrue(input.enrichment().isEmpty());
    }

    @Test
    void withEnrichment_deberiaCrearNuevaInstanciaConservandoCampos() {
        var base = TestReportInputs.input();
        var enriched = base.withEnrichment(someEnrichment());

        assertEquals(base.projectId(), enriched.projectId());
        assertEquals(base.projectTitle(), enriched.projectTitle());
        assertEquals(base.projectContext(), enriched.projectContext());
        assertEquals(base.evaluation(), enriched.evaluation());
        assertEquals(base.score(), enriched.score());
        assertEquals(base.recommendation(), enriched.recommendation());
        assertEquals(base.risk(), enriched.risk());
        assertEquals(base.opportunity(), enriched.opportunity());
        assertFalse(enriched.enrichment().isEmpty());
        assertTrue(base.enrichment().isEmpty());
    }

    private static EnrichmentResult someEnrichment() {
        var fact = KnowledgeFact.of("El mercado crece 15% anual", "src-1", "https://example.com",
            OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "sector");
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(new KnowledgeEvidence(fact,
                EvidenceScore.of(0.8, EvidenceCategory.MARKET, "Relevante."))));
        return new EnrichmentResult(List.of(rank), List.of("src-1"), 0.8,
            "enriquecido", "Test", "v1");
    }
}
