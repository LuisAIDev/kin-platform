package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.RecommendationEngine;
import com.kinplatform.kin.reporting.RecommendationInput;
import com.kinplatform.kin.reporting.RecommendationModel;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EngineStageTest {

    private final EngineStage<RecommendationInput, RecommendationResult> stage = new EngineStage<>(
        "Recomendaciones",
        new RecommendationEngine(RecommendationModel.defaultModel()),
        context -> context.projectContext() != null
            && context.decision() != null
            && context.decision().shouldGenerateReport(),
        context -> new RecommendationInput(
            context.projectContext(),
            context.evaluation(),
            context.decision(),
            context.scoreResult()
        ),
        PipelineContext::recommendationResult);

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
    void name_deberiaSerElNombreConfigurado() {
        assertEquals("Recomendaciones", stage.name());
    }

    @Test
    void supports_deberiaEvaluarElPredicado() {
        assertFalse(stage.supports(context(ConversationDecision.ask(AnalyzedDimension.MVP, 5, "preguntar"),
            ScoreResult.empty())));
        assertTrue(stage.supports(context(ConversationDecision.generateReport("reporte"), ScoreResult.empty())));
    }

    @Test
    void execute_deberiaEscribirElResultadoTipadoYEnElMapaGenrico() {
        var ctx = context(ConversationDecision.generateReport("reporte"), ScoreResult.empty());
        var result = stage.execute(ctx);

        assertSame(ctx, result);
        assertNotNull(ctx.recommendationResult());
        assertEquals(RecommendationEngine.GENERATOR_NAME, ctx.recommendationResult().generatedBy());

        var generic = ctx.<RecommendationResult>engineResult("RecommendationEngine");
        assertNotNull(generic);
        assertEquals(ctx.recommendationResult(), generic);
        assertEquals(1, ctx.engineResults().size());
    }
}
