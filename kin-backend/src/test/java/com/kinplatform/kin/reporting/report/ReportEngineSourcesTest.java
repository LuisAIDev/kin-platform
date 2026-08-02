package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.EvidenceScore;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.reporting.report.assembler.ExecutiveSummaryAssembler;
import com.kinplatform.kin.reporting.report.assembler.FinancialSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.InnovationSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.MarketSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.NextStepsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.OpportunitiesSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.RecommendationsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ReportMetadataAssembler;
import com.kinplatform.kin.reporting.report.assembler.RisksSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ScoresSectionAssembler;
import com.kinplatform.kin.reporting.report.model.SourcesSection;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportEngineSourcesTest {

    private ReportEngine engine() {
        var model = ReportModel.defaultModel();
        var assemblers = new ReportAssemblers(
            new ExecutiveSummaryAssembler(),
            new ScoresSectionAssembler(),
            new RecommendationsSectionAssembler(),
            new RisksSectionAssembler(),
            new OpportunitiesSectionAssembler(),
            new FinancialSectionAssembler(),
            new MarketSectionAssembler(),
            new InnovationSectionAssembler(),
            new NextStepsSectionAssembler(model),
            new ReportMetadataAssembler(model));
        return new ReportEngine(assemblers, model);
    }

    @Test
    void conEnriquecimiento_deberiaIncluirSeccionDeFuentes() {
        var enrichment = enrichmentFor(EvidenceCategory.MARKET,
            "El mercado crece 15%", "src-1", "https://x/1", 0.8);
        var report = engine().evaluate(TestReportInputs.input().withEnrichment(enrichment));

        assertFalse(report.sources().isEmpty());
        assertEquals(1, report.sources().sources().size());
        assertEquals("src-1", report.sources().sources().get(0).sourceId());
        assertTrue(report.sectionsInOrder().stream().anyMatch(s -> s instanceof SourcesSection));
        assertTrue(report.metadata().sectionsIncluded().contains("Sources"));
    }

    @Test
    void sinEnriquecimiento_deberiaComportarseComoAntes() {
        var report = engine().evaluate(TestReportInputs.input());

        assertTrue(report.sources().isEmpty());
        assertFalse(report.sectionsInOrder().stream().anyMatch(s -> s instanceof SourcesSection));
        assertFalse(report.metadata().sectionsIncluded().contains("Sources"));
        assertEquals(10, report.metadata().sectionsIncluded().size());
    }

    private static EnrichmentResult enrichmentFor(EvidenceCategory category, String claim,
                                                  String sourceId, String url, double score) {
        var fact = KnowledgeFact.of(claim, sourceId, url, OffsetDateTime.now(),
            SourceTrust.OFFICIAL_PUBLIC, "sector");
        var rank = EvidenceRank.of(category,
            List.of(new KnowledgeEvidence(fact, EvidenceScore.of(score, category, "Relevante."))));
        return new EnrichmentResult(List.of(rank), List.of(sourceId), score,
            "enriquecido", "Test", "v1");
    }
}
