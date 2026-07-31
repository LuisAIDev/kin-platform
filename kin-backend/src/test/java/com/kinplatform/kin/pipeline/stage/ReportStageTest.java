package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.report.ReportAssemblers;
import com.kinplatform.kin.reporting.report.ReportEngine;
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
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportStageTest {

    private final ReportStage stage = new ReportStage(engine());

    private static ReportEngine engine() {
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

    private PipelineContext context(ConversationDecision decision) {
        var ctx = new PipelineContext(
            UUID.randomUUID(), UUID.randomUUID(), "mensaje", List.of(),
            "Proyecto", "Descripci\u00F3n", "Tecnolog\u00EDa");
        ctx.projectContext(ProjectContext.fromProject("Proyecto", "Descripci\u00F3n", "Tecnolog\u00EDa"));
        ctx.evaluation(new CompletenessEvaluation(
            0.5, List.of(AnalyzedDimension.MVP), List.of(),
            0.7, CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, 0.6,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 8, AnalyzedDimension.values().length));
        ctx.decision(decision);
        ctx.scoreResult(ScoreResult.empty());
        ctx.recommendationResult(RecommendationResult.empty());
        ctx.riskResult(RiskResult.empty());
        ctx.opportunityResult(OpportunityResult.empty());
        return ctx;
    }

    @Test
    void name_deberiaSerReporte() {
        assertEquals("Reporte", stage.name());
    }

    @Test
    void supports_deberiaSerFalso_cuandoNoHayReporte() {
        var ctx = context(ConversationDecision.ask(AnalyzedDimension.MVP, 5, "preguntar"));
        assertFalse(stage.supports(ctx));
    }

    @Test
    void supports_deberiaSerFalso_cuandoFaltaUnResultado() {
        var ctx = context(ConversationDecision.generateReport("reporte"));
        ctx.opportunityResult(null);
        assertFalse(stage.supports(ctx));
        ctx.opportunityResult(OpportunityResult.empty());
        ctx.riskResult(null);
        assertFalse(stage.supports(ctx));
        ctx.riskResult(RiskResult.empty());
        ctx.scoreResult(null);
        assertFalse(stage.supports(ctx));
    }

    @Test
    void supports_deberiaSerVerdadero_cuandoLosCuatroResultadosEstanPresentes() {
        var ctx = context(ConversationDecision.generateReport("reporte"));
        assertTrue(stage.supports(ctx));
    }

    @Test
    void execute_deberiaGuardarElReporteEnContexto() {
        var ctx = context(ConversationDecision.generateReport("reporte"));
        var result = stage.execute(ctx);
        assertSame(ctx, result);
        ConsultingReport report = ctx.consultingReport();
        assertNotNull(report);
        assertEquals(ReportEngine.GENERATOR_NAME, report.generatedBy());
        assertFalse(report.isEmpty());
        assertNotNull(ctx.engineResult(ReportEngine.GENERATOR_NAME));
        assertEquals(report, ctx.engineResult(ReportEngine.GENERATOR_NAME));
    }
}
