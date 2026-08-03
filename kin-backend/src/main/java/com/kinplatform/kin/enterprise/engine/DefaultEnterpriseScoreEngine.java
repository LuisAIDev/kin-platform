package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.EnterpriseScoreInput;
import com.kinplatform.kin.enterprise.engine.result.EnterpriseScoreResult;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;

/**
 * Implementación determinista del {@link EnterpriseScoreEngine} (Fase 10,
 * Milestone 2D).
 *
 * <p>Calcula el {@link EnterpriseScore} multidimensional a partir de los value
 * objects y resultados del pipeline ya producidos. <b>No inventa datos</b>: cada
 * dimensión se deriva de forma determinista de las entradas disponibles.
 * Reglas funcionales de puntuación (0-100 cada dimensión):</p>
 *
 * <ul>
 *   <li><b>Market</b>: media de (proporción SOM/TAM, tasa de crecimiento
 *       limitada a 100 y confianza del plan de mercado).</li>
 *   <li><b>Innovation</b>: nivel del plan de innovación
 *       ({@code INCREMENTAL=40}, {@code TRANSFORMATIONAL=70},
 *       {@code DISRUPTIVE=90}) con bonificación de 5 si hay diferenciadores.</li>
 *   <li><b>Viability</b>: ratio de cobertura del contexto
 *       ({@code coverageRatio} × 100).</li>
 *   <li><b>Financial</b>: media del (100 − proporción del mes de equilibrio
 *       sobre 24 meses; 0 si no hay punto de equilibrio) y el margen bruto.</li>
 *   <li><b>Risk</b>: nivel global de riesgo
 *       ({@code LOW=90}, {@code MEDIUM=65}, {@code HIGH=40},
 *       {@code CRITICAL=20}) ponderado por la confianza del resultado de
 *       riesgo.</li>
 *   <li><b>Scalability</b>: 70 si la dimensión está cubierta en el contexto,
 *       40 en caso contrario.</li>
 *   <li><b>Team</b>: 70 si la dimensión de recursos está cubierta en el
 *       contexto, 40 en caso contrario.</li>
 *   <li><b>Sustainability</b>: 40 por defecto más hasta 40 puntos según el
 *       número de hechos de conocimiento verificados.</li>
 *   <li>La confianza global es la media de las confianzas disponibles
 *       (mercado, recomendaciones, oportunidades, conocimiento y riesgo).</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultEnterpriseScoreEngine implements EnterpriseScoreEngine {

    private static final String ENGINE_NAME = "kin.enterprise:EnterpriseScore";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 87;

    private static final double INNOVATION_INCREMENTAL = 40.0;
    private static final double INNOVATION_TRANSFORMATIONAL = 70.0;
    private static final double INNOVATION_DISRUPTIVE = 90.0;
    private static final double DIFFERENTIATOR_BONUS = 5.0;
    private static final double COVERED_SCORE = 70.0;
    private static final double UNCOVERED_SCORE = 40.0;
    private static final int BREAK_EVEN_HORIZON_MONTHS = 24;
    private static final double RISK_LOW = 90.0;
    private static final double RISK_MEDIUM = 65.0;
    private static final double RISK_HIGH = 40.0;
    private static final double RISK_CRITICAL = 20.0;
    private static final double BASE_SUSTAINABILITY = 40.0;
    private static final double SUSTAINABILITY_PER_FACT = 20.0;

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.SCORING, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public EnterpriseScoreResult evaluate(EnterpriseScoreInput input) {
        if (input == null || input.context() == null) {
            return EnterpriseScoreResult.empty();
        }
        var context = input.context();

        double market = marketScore(input.marketPlan());
        double innovation = innovationScore(input.innovationPlan());
        double viability = viabilityScore(context);
        double financial = financialScore(input.financialPlan());
        double risk = riskScore(input.riskResult());
        double scalability = context.isDimensionCovered(
            com.kinplatform.kin.context.AnalyzedDimension.SCALABILITY)
            ? COVERED_SCORE : UNCOVERED_SCORE;
        double team = context.isDimensionCovered(
            com.kinplatform.kin.context.AnalyzedDimension.RESOURCES)
            ? COVERED_SCORE : UNCOVERED_SCORE;
        double sustainability = sustainabilityScore(input.knowledge());

        double confidence = aggregateConfidence(input);
        var score = EnterpriseScore.calculate(market, innovation, viability, financial,
            risk, scalability, team, sustainability, confidence);

        String explanation = buildExplanation(score, input);

        return new EnterpriseScoreResult(score, confidence, explanation,
            "EnterpriseScoreEngine", ENGINE_VERSION);
    }

    private double marketScore(MarketPlan marketPlan) {
        if (marketPlan == null) {
            return 0.0;
        }
        double sizeScore = marketPlan.tam() > 0.0
            ? Math.min(100.0, (marketPlan.som() / marketPlan.tam()) * 100.0) : 0.0;
        double growthScore = Math.min(100.0, marketPlan.growthRate());
        double confidenceScore = marketPlan.confidence() * 100.0;
        return clamp((sizeScore + growthScore + confidenceScore) / 3.0);
    }

    private double innovationScore(InnovationPlan plan) {
        if (plan == null) {
            return 0.0;
        }
        double base = switch (plan.innovationLevel()) {
            case INCREMENTAL -> INNOVATION_INCREMENTAL;
            case TRANSFORMATIONAL -> INNOVATION_TRANSFORMATIONAL;
            case DISRUPTIVE -> INNOVATION_DISRUPTIVE;
        };
        if (!plan.differentiators().isEmpty()) {
            base += DIFFERENTIATOR_BONUS;
        }
        return clamp(base);
    }

    private double viabilityScore(ProjectContext context) {
        return clamp(context.coverageRatio() * 100.0);
    }

    private double financialScore(FinancialPlan plan) {
        if (plan == null) {
            return 0.0;
        }
        double breakEvenScore = plan.breakEvenMonth() > 0
            ? Math.max(0.0, 100.0 - (plan.breakEvenMonth() / (double) BREAK_EVEN_HORIZON_MONTHS) * 100.0)
            : 0.0;
        double marginScore = plan.grossMargin();
        return clamp((breakEvenScore + marginScore) / 2.0);
    }

    private double riskScore(RiskResult riskResult) {
        if (riskResult == null) {
            return 0.0;
        }
        double levelScore = switch (riskResult.overallRiskLevel()) {
            case LOW -> RISK_LOW;
            case MEDIUM -> RISK_MEDIUM;
            case HIGH -> RISK_HIGH;
            case CRITICAL -> RISK_CRITICAL;
        };
        return clamp(levelScore * (0.5 + 0.5 * riskResult.confidence()));
    }

    private double sustainabilityScore(KnowledgeResult knowledge) {
        if (knowledge == null) {
            return BASE_SUSTAINABILITY;
        }
        int count = knowledge.facts().size();
        return clamp(BASE_SUSTAINABILITY + count * SUSTAINABILITY_PER_FACT);
    }

    private double aggregateConfidence(EnterpriseScoreInput input) {
        double sum = 0.0;
        int sources = 0;
        if (input.marketPlan() != null) {
            sum += input.marketPlan().confidence();
            sources++;
        }
        if (input.recommendations() != null) {
            sum += input.recommendations().confidence();
            sources++;
        }
        if (input.opportunities() != null) {
            sum += input.opportunities().confidence();
            sources++;
        }
        if (input.knowledge() != null) {
            sum += input.knowledge().confidence();
            sources++;
        }
        if (input.riskResult() != null) {
            sum += input.riskResult().confidence();
            sources++;
        }
        return sources == 0 ? 0.0 : clamp(sum / sources);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private String buildExplanation(EnterpriseScore score, EnterpriseScoreInput input) {
        if (input.marketPlan() == null) {
            return "Puntuación empresarial parcial: faltan datos de mercado.";
        }
        return "Enterprise Score calculado: " + score.overallScore() + "/100 ("
            + score.grade().name() + ") a partir de los planes del proyecto.";
    }
}
