package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationCategory;
import com.kinplatform.kin.reporting.RecommendationExplanation;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityExplanation;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fábrica de entradas de prueba para los tests del reporte.
 */
public final class TestReportInputs {

    public static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TestReportInputs() {
    }

    public static ProjectContext context() {
        var data = Map.<AnalyzedDimension, String>of(
            AnalyzedDimension.SECTOR, "tech",
            AnalyzedDimension.TARGET_CUSTOMER, "pymes",
            AnalyzedDimension.CITY, "Buenos Aires",
            AnalyzedDimension.PROBLEM, "falta software",
            AnalyzedDimension.SOLUTION, "app",
            AnalyzedDimension.VALUE_PROPOSITION, "ahorro",
            AnalyzedDimension.MVP, "piloto",
            AnalyzedDimension.REVENUE_MODEL, "suscripcion",
            AnalyzedDimension.RESOURCES, "equipo",
            AnalyzedDimension.OBJECTIVES, "crecer");
        return ProjectContext.restore(data, data.keySet(), null, 5, false);
    }

    public static CompletenessEvaluation evaluation() {
        return new CompletenessEvaluation(
            0.6, List.of(AnalyzedDimension.RISKS), List.of(), 0.75,
            CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.HIGH, 0.7,
            List.of("riesgo X"), List.of("automatizar procesos", "innovaci\u00F3n en distribuci\u00F3n"),
            List.of(), CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT, true, 10, 14);
    }

    public static ScoreResult score() {
        return new ScoreResult(78, 100, Map.of("Mercado", 80), "VIABLE",
            List.of("fortaleza 1"), List.of("debilidad 1"), "explicacion");
    }

    public static RecommendationResult recommendation() {
        return new RecommendationResult(
            List.of(recommendation("Recomendaci\u00F3n A", 9), recommendation("Recomendaci\u00F3n B", 5)),
            9, 0.8, RecommendationCategory.STRATEGY, "exp", "RecommendationEngine", "v1");
    }

    public static Recommendation recommendation(String title, int priority) {
        return Recommendation.create(RecommendationCategory.STRATEGY, title, "desc", priority,
            ImpactLevel.HIGH, EffortLevel.LOW, AnalyzedDimension.PROBLEM, List.of("paso"),
            "resultado", RecommendationExplanation.of(List.of("dato"), "regla-recomendacion", "motivo"));
    }

    public static RiskResult risk() {
        var top = risk("Riesgo A", RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.MEDIUM);
        return new RiskResult(List.of(top), RiskLevel.HIGH, List.of(top), 0.7,
            "exp", "RiskEngine", "v1");
    }

    public static Risk risk(String title, RiskLevel severity, RiskLevel probability, RiskLevel impact) {
        return Risk.create(RiskCategory.BUSINESS, title, "desc", severity, probability, impact, 0.7,
            RiskExplanation.of(List.of("dato"), "regla-riesgo", "motivo", "evidencia"),
            List.of("R1"), AnalyzedDimension.RISKS, "v1");
    }

    public static OpportunityResult opportunity() {
        return new OpportunityResult(
            List.of(opportunity("Oportunidad A", 8), opportunity("Oportunidad B", 4)),
            List.of(opportunity("Oportunidad A", 8)), 0.8, "exp", "OpportunityEngine", "v1");
    }

    public static Opportunity opportunity(String title, int priority) {
        return Opportunity.create(OpportunityCategory.MERCADO, title, "desc", priority,
            ImpactLevel.HIGH, EffortLevel.MEDIUM, 0.8,
            OpportunityExplanation.of(List.of("dato"), "regla-oportunidad", "motivo", "evidencia"),
            List.of("R1"), AnalyzedDimension.PROBLEM, "v1");
    }

    public static ReportInput input() {
        return new ReportInput(PROJECT_ID, "Proyecto", "tech", context(), evaluation(),
            ConversationDecision.generateReport("ok"), score(), recommendation(), risk(), opportunity());
    }
}
