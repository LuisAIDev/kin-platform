package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.report.model.ReportSection;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportInputTest {

    @Test
    void input_deberiaPortarTodosLosCampos() {
        var projectId = UUID.randomUUID();
        var context = ProjectContext.fromProject("Proyecto", "desc", "tech");
        var evaluation = CompletenessEvaluation.empty();
        var decision = ConversationDecision.generateReport("ok");
        var score = ScoreResult.empty();
        var recommendation = RecommendationResult.empty();
        var risk = RiskResult.empty();
        var opportunity = OpportunityResult.empty();

        var input = new ReportInput(projectId, "Proyecto", "tech", context, evaluation,
            decision, score, recommendation, risk, opportunity);

        assertEquals(projectId, input.projectId());
        assertEquals("Proyecto", input.projectTitle());
        assertEquals("tech", input.projectCategory());
        assertSame(context, input.projectContext());
        assertSame(evaluation, input.evaluation());
        assertSame(decision, input.decision());
        assertSame(score, input.score());
        assertSame(recommendation, input.recommendation());
        assertSame(risk, input.risk());
        assertSame(opportunity, input.opportunity());
        assertTrue(input instanceof com.kinplatform.kin.engine.EngineInput);
    }

    @Test
    void sectionAssembler_deberiaAceptarUnaImplementacion() {
        SectionAssembler<ReportSection> assembler = input ->
            new ReportSection() {
                @Override
                public String sectionName() {
                    return "Generica";
                }
            };
        var input = new ReportInput(UUID.randomUUID(), "t", "c", null, null, null,
            null, null, null, null);
        assertEquals("Generica", assembler.assemble(input).sectionName());
        assertEquals(AnalyzedDimension.PROJECT_NAME, AnalyzedDimension.PROJECT_NAME);
    }
}
