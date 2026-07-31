package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.risk.BusinessRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.MarketRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.RiskEngine;
import com.kinplatform.kin.reporting.risk.RiskModel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RiskStageTest {

    private final RiskStage stage = new RiskStage(new RiskEngine(
        List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
        RiskModel.defaultModel()));

    private PipelineContext context(ConversationDecision decision, ScoreResult score) {
        var ctx = new PipelineContext(
            UUID.randomUUID(), UUID.randomUUID(), "mensaje", List.of(),
            "Proyecto", "Descripción", "Tecnología");
        ctx.projectContext(ProjectContext.fromProject("Proyecto", "Descripción", "Tecnología"));
        ctx.evaluation(new CompletenessEvaluation(
            0.5, List.of(AnalyzedDimension.MVP), List.of(),
            0.7, CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, 0.6,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 8, AnalyzedDimension.values().length));
        ctx.decision(decision);
        ctx.scoreResult(score);
        return ctx;
    }

    @Test
    void name_deberiaSerRiesgos() {
        assertEquals("Riesgos", stage.name());
    }

    @Test
    void supports_deberiaSerFalso_cuandoNoHayReporte() {
        var ctx = context(ConversationDecision.ask(AnalyzedDimension.MVP, 5, "preguntar"),
            ScoreResult.empty());
        assertFalse(stage.supports(ctx));
    }

    @Test
    void supports_deberiaSerFalso_cuandoNoHayScore() {
        var ctx = context(ConversationDecision.generateReport("reporte"), null);
        ctx.scoreResult(null);
        assertFalse(stage.supports(ctx));
    }

    @Test
    void supports_deberiaSerVerdadero_cuandoReporteYScorePresentes() {
        var ctx = context(ConversationDecision.generateReport("reporte"), ScoreResult.empty());
        assertTrue(stage.supports(ctx));
    }

    @Test
    void execute_deberiaGuardarRiesgosEnContexto() {
        var ctx = context(ConversationDecision.generateReport("reporte"), ScoreResult.empty());
        var result = stage.execute(ctx);
        assertSame(ctx, result);
        RiskResult rr = ctx.riskResult();
        assertNotNull(rr);
        assertEquals(RiskEngine.GENERATOR_NAME, rr.generatedBy());
        assertTrue(rr.risks().stream()
            .allMatch(r -> r.category() == com.kinplatform.kin.reporting.risk.RiskCategory.BUSINESS
                || r.category() == com.kinplatform.kin.reporting.risk.RiskCategory.MARKET));
    }
}
