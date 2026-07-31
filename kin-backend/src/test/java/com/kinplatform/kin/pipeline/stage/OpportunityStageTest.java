package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.opportunity.MarketOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.MonetizationOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityEngine;
import com.kinplatform.kin.reporting.opportunity.OpportunityModel;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpportunityStageTest {

    private final OpportunityStage stage = new OpportunityStage(new OpportunityEngine(
        List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
        OpportunityModel.defaultModel()));

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
    void name_deberiaSerOportunidades() {
        assertEquals("Oportunidades", stage.name());
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
    void execute_deberiaGuardarOportunidadesEnContexto() {
        var ctx = context(ConversationDecision.generateReport("reporte"), ScoreResult.empty());
        var result = stage.execute(ctx);
        assertSame(ctx, result);
        OpportunityResult or = ctx.opportunityResult();
        assertNotNull(or);
        assertEquals(OpportunityEngine.GENERATOR_NAME, or.generatedBy());
        assertTrue(or.opportunities().stream()
            .allMatch(o -> o.category() == OpportunityCategory.MERCADO
                || o.category() == OpportunityCategory.MONETIZACION));
        // El resultado también queda registrado en engineResults por nombre
        assertNotNull(ctx.engineResult(OpportunityEngine.GENERATOR_NAME));
    }
}
