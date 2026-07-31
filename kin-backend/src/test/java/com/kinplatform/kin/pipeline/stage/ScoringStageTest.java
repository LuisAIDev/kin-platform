package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.scoring.ScoringEngine;
import com.kinplatform.kin.scoring.ScoringModel;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScoringStageTest {

    private final ScoringStage stage =
        new ScoringStage(new ScoringEngine(ScoringModel.defaultModel()));

    private PipelineContext context(ConversationDecision decision) {
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
        return ctx;
    }

    @Test
    void name_deberiaSerScoring() {
        assertEquals("Scoring", stage.name());
    }

    @Test
    void supports_deberiaSerFalso_cuandoNoHayReporte() {
        assertFalse(stage.supports(context(ConversationDecision.ask(AnalyzedDimension.MVP, 5, "preguntar"))));
    }

    @Test
    void supports_deberiaSerVerdadero_cuandoHayReporte() {
        assertTrue(stage.supports(context(ConversationDecision.generateReport("reporte"))));
    }

    @Test
    void execute_deberiaGuardarElScoreEnContextoYEnElMapaGenerico() {
        var ctx = context(ConversationDecision.generateReport("reporte"));
        var result = stage.execute(ctx);

        assertSame(ctx, result);
        assertNotNull(ctx.scoreResult());
        assertEquals("ScoringEngine", ctx.scoreResult().generatedBy());

        var generic = ctx.<ScoreResult>engineResult("ScoringEngine");
        assertNotNull(generic);
        assertEquals(ctx.scoreResult(), generic);
    }
}
