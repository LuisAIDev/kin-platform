package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter;
import com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.SourcesSectionFormatter;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.enrichment.FactRanker;
import com.kinplatform.kin.enrichment.stage.EnrichmentStage;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.report.ReportAssemblers;
import com.kinplatform.kin.reporting.report.ReportEngine;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.ReportModel;
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
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración (cierre del defecto de la Auditoría E7): reproduce el
 * flujo completo KnowledgeResult con hechos → EnrichmentStage → ReportEngine →
 * ConsultingReport con SourcesSection → PromptAssembler → ReportPromptBuilder,
 * y verifica que el prompt REPORT se genera sin IllegalArgumentException y que
 * el {@link SourcesSectionFormatter} es utilizado.
 */
class ReportPromptSourcesIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void flujoCompleto_deberiaGenerarPromptConFuentes_sinIllegalArgumentException() {
        KnowledgeResult knowledge = knowledgeConHechos();

        PipelineContext ctx = new PipelineContext(PROJECT_ID, USER_ID, "generá el informe",
            List.of(), "Proyecto Test", "Descripción", "Software");
        ctx.projectContext(ProjectContext.fromProject("Proyecto Test", "Descripción", "Software"));
        ctx.knowledgeResult(knowledge);
        new EnrichmentStage(new EnrichmentEngine(new FactRanker())).execute(ctx);

        assertNotNull(ctx.enrichmentResult());
        assertFalse(ctx.enrichmentResult().isEmpty());

        var report = reportEngine().evaluate(enrichedReportInput(ctx));
        assertFalse(report.sources().isEmpty());
        assertTrue(report.sectionsInOrder().stream().anyMatch(s -> s instanceof SourcesSection));

        var promptAssembler = new PromptAssembler(new ConversationPromptBuilder(), reportPromptBuilder());
        String prompt = promptAssembler.assemble(PromptRequest.forReport(report));

        assertNotNull(prompt);
        assertTrue(prompt.contains("## Fuentes Citadas"));
        assertTrue(prompt.contains("https://example.com/reporte"));
        assertTrue(prompt.contains("**Categoría:** Mercado"));
        assertTrue(prompt.contains("**Total:** 1 fuentes citadas"));
    }

    private KnowledgeResult knowledgeConHechos() {
        var fact = KnowledgeFact.of(
            "El mercado retail crece con demanda del consumidor. Dato verificado.",
            "src-1", "https://example.com/reporte", OffsetDateTime.now().minusDays(10),
            SourceTrust.OFFICIAL_PUBLIC, "mercado");
        return new KnowledgeResult(List.of(fact), List.of("src-1"), List.of(),
            0.9, "conocimiento", "KnowledgeEngine", "v1");
    }

    private ReportInput enrichedReportInput(PipelineContext ctx) {
        var input = new ReportInput(PROJECT_ID, "Proyecto Test", "Software",
            ctx.projectContext(), evaluation(), ConversationDecision.generateReport("reporte"),
            ScoreResult.empty(), RecommendationResult.empty(), RiskResult.empty(),
            OpportunityResult.empty());
        return input.withEnrichment(ctx.enrichmentResult());
    }

    private CompletenessEvaluation evaluation() {
        return new CompletenessEvaluation(
            0.5, List.of(AnalyzedDimension.MVP), List.of(),
            0.7, CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, 0.6,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 8, AnalyzedDimension.values().length);
    }

    private ReportEngine reportEngine() {
        var model = ReportModel.defaultModel();
        return new ReportEngine(new ReportAssemblers(
            new ExecutiveSummaryAssembler(),
            new ScoresSectionAssembler(),
            new RecommendationsSectionAssembler(),
            new RisksSectionAssembler(),
            new OpportunitiesSectionAssembler(),
            new FinancialSectionAssembler(),
            new MarketSectionAssembler(),
            new InnovationSectionAssembler(),
            new NextStepsSectionAssembler(model),
            new ReportMetadataAssembler(model)), model);
    }

    private ReportPromptBuilder reportPromptBuilder() {
        return new ReportPromptBuilder(List.of(
            new ExecutiveSummaryFormatter(),
            new ScoresSectionFormatter(),
            new RecommendationsSectionFormatter(),
            new RisksSectionFormatter(),
            new OpportunitiesSectionFormatter(),
            new FinancialSectionFormatter(),
            new MarketSectionFormatter(),
            new InnovationSectionFormatter(),
            new NextStepsSectionFormatter(),
            new ReportMetadataFormatter(),
            new SourcesSectionFormatter()));
    }
}
