package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades de monetización (MONETIZACION). Detecta la
 * brecha del modelo de ingresos como oportunidad de monetización del proyecto.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class MonetizationOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.MONETIZACION;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.REVENUE_MODEL)) {
            opportunities.add(buildOpportunity(
                "Diseñar el modelo de monetización",
                "Definir cómo generará ingresos el proyecto convierte el valor en sostenibilidad.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.REVENUE_MODEL),
                ImpactLevel.CRITICAL, EffortLevel.HIGH,
                List.of("REVENUE_MODEL_NO_CUBIERTO"),
                AnalyzedDimension.REVENUE_MODEL,
                "Dimensión REVENUE_MODEL no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "monetiz")) {
            opportunities.add(buildOpportunity(
                "Explotar la señal de monetización detectada",
                "La evaluación detectó una oportunidad de monetización que conviene implementar.",
                priorityFromScore, 2, 0,
                ImpactLevel.CRITICAL, EffortLevel.HIGH,
                List.of("SEÑAL_MONETIZACION_DETECTADA"),
                AnalyzedDimension.REVENUE_MODEL,
                "Señal detectada por la evaluación de completitud",
                evaluation));
        }

        return opportunities;
    }

    @Override
    public String version() {
        return VERSION;
    }

    private Opportunity buildOpportunity(String title, String description, int priorityFromScore,
                                         int detectedBonus, int missingBonus,
                                         ImpactLevel impact, EffortLevel effort, List<String> rules,
                                         AnalyzedDimension dimension, String evidence,
                                         CompletenessEvaluation evaluation) {
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad de monetización.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
