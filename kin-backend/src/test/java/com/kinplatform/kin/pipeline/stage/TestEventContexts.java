package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fábricas de {@link PipelineContext} y resultados para los tests de
 * {@link EventStage} (E4).
 */
final class TestEventContexts {

    private TestEventContexts() {
    }

    static PipelineContext context() {
        var ctx = new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
        ctx.projectContext(ProjectContext.fromProject("t", "d", "c"));
        return ctx;
    }

    static PipelineContext ask(PipelineContext ctx) {
        ctx.decision(ConversationDecision.ask(AnalyzedDimension.SECTOR, 5, "pregunta"));
        return ctx;
    }

    static PipelineContext report(PipelineContext ctx) {
        ctx.decision(ConversationDecision.generateReport("informe"));
        return ctx;
    }

    static PipelineContext withScore(PipelineContext ctx, int totalScore) {
        ctx.scoreResult(new ScoreResult(totalScore, 100, Map.of(), "VIABLE", List.of(), List.of(), "ok"));
        return ctx;
    }

    static PipelineContext withRisks(PipelineContext ctx, Risk... risks) {
        ctx.riskResult(new RiskResult(List.of(risks), RiskLevel.LOW, List.of(),
            0.8, "exp", "RiskEngine", "v1"));
        return ctx;
    }

    static PipelineContext withReport(PipelineContext ctx) {
        ctx.consultingReport(ConsultingReport.empty());
        return ctx;
    }

    static Risk risk(String title, RiskLevel severity) {
        return new Risk(null, RiskCategory.MARKET, title, "desc", severity,
            RiskLevel.MEDIUM, RiskLevel.MEDIUM, 0.8,
            RiskExplanation.of(List.of(), "rule", "reason", "evidence"),
            List.of("rule"), AnalyzedDimension.COMPETITION, "RiskEngine");
    }
}
