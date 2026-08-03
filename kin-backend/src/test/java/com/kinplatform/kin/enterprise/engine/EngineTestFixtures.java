package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
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

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixtures de prueba compartidos por los tests de los engines empresariales
 * (Fase 10, Milestone 2D).
 */
public final class EngineTestFixtures {

    private EngineTestFixtures() {
    }

    public static ProjectContext context(Map<AnalyzedDimension, String> dims) {
        var ctx = ProjectContext.fromProject("Proyecto Test", "Solución", "Tecnología");
        var extra = new LinkedHashMap<AnalyzedDimension, String>(dims);
        extra.remove(AnalyzedDimension.PROJECT_NAME);
        extra.remove(AnalyzedDimension.SECTOR);
        if (!extra.isEmpty()) {
            ctx.update(new AnalysisResult(extra));
        }
        return ctx;
    }

    public static ProjectContext contextWithAll() {
        var map = new LinkedHashMap<AnalyzedDimension, String>();
        map.put(AnalyzedDimension.PROBLEM, "Problema claro del cliente");
        map.put(AnalyzedDimension.TARGET_CUSTOMER, "Pymes del sector retail");
        map.put(AnalyzedDimension.VALUE_PROPOSITION, "Ahorro de costes operativos");
        map.put(AnalyzedDimension.SOLUTION, "Plataforma SaaS de gestión");
        map.put(AnalyzedDimension.REVENUE_MODEL, "Suscripción mensual");
        map.put(AnalyzedDimension.SCALABILITY, "Escalable a otros sectores");
        map.put(AnalyzedDimension.RESOURCES, "Equipo de 5 personas");
        return context(map);
    }

    public static KnowledgeResult knowledge(double confidence) {
        return new KnowledgeResult(
            List.of(
                KnowledgeFact.of("El mercado alcanza 1000000 unidades al año", "src1",
                    "https://example.com/a", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "mercado"),
                KnowledgeFact.of("Crecimiento anual del 15 por ciento", "src2",
                    "https://example.com/b", OffsetDateTime.now(), SourceTrust.SECONDARY, "crecimiento")),
            List.of("src1", "src2"), List.of(), confidence,
            "Conocimiento verificado.", "KnowledgeEngine", "1.0.0");
    }

    public static KnowledgeResult knowledgeEmpty() {
        return KnowledgeResult.empty();
    }

    public static RecommendationResult recommendations(double confidence) {
        var rec = Recommendation.create(
            RecommendationCategory.VALIDATION, "Validar hipótesis de demanda", "Descripción",
            8, ImpactLevel.HIGH, EffortLevel.MEDIUM, AnalyzedDimension.MVP,
            List.of("Entrevistar a 10 clientes"), "Validación temprana",
            RecommendationExplanation.of(List.of("dato"), "regla", "razón"));
        return new RecommendationResult(List.of(rec), 8, confidence,
            RecommendationCategory.VALIDATION, "Recomendaciones generadas.",
            "RecommendationEngine", "1.0.0");
    }

    public static RecommendationResult recommendationsEmpty() {
        return RecommendationResult.empty();
    }

    public static OpportunityResult opportunities(double confidence) {
        var oppInnov = Opportunity.create(OpportunityCategory.INNOVACION, "Innovación de proceso",
            "Descripción", 8, ImpactLevel.HIGH, EffortLevel.MEDIUM, confidence,
            OpportunityExplanation.of(List.of(), "regla", "razón", "evidencia"),
            List.of("r1"), AnalyzedDimension.SCALABILITY, "1.0.0");
        var oppTec = Opportunity.create(OpportunityCategory.TECNOLOGICA, "Tecnología diferenciadora",
            "Descripción", 7, ImpactLevel.HIGH, EffortLevel.LOW, confidence,
            OpportunityExplanation.of(List.of(), "regla", "razón", "evidencia"),
            List.of("r1"), AnalyzedDimension.SOLUTION, "1.0.0");
        var oppComp = Opportunity.create(OpportunityCategory.COMPETITIVA, "Ventaja competitiva",
            "Descripción", 6, ImpactLevel.MEDIUM, EffortLevel.MEDIUM, confidence,
            OpportunityExplanation.of(List.of(), "regla", "razón", "evidencia"),
            List.of("r1"), AnalyzedDimension.COMPETITION, "1.0.0");
        return new OpportunityResult(List.of(oppInnov, oppTec, oppComp), List.of(oppInnov, oppTec),
            confidence, "Oportunidades identificadas.", "OpportunityEngine", "1.0.0");
    }

    public static OpportunityResult opportunitiesEmpty() {
        return OpportunityResult.empty();
    }

    public static RiskResult riskResult(double confidence) {
        var risk = Risk.create(RiskCategory.FINANCIAL, "Riesgo financiero", "Descripción",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH, confidence,
            RiskExplanation.of(List.of("dato"), "regla", "razón", "evidencia"),
            List.of("r1"), AnalyzedDimension.REVENUE_MODEL, "1.0.0");
        return new RiskResult(List.of(risk), RiskLevel.HIGH, List.of(risk), confidence,
            "Riesgos identificados.", "RiskEngine", "1.0.0");
    }

    public static RiskResult riskResultEmpty() {
        return RiskResult.empty();
    }
}
