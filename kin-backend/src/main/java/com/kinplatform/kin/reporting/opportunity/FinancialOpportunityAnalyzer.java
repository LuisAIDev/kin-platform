package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades financieras (FINANCIERA). Detecta brechas en
 * objetivos y recursos que representan oportunidades de saneamiento y
 * crecimiento financiero.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class FinancialOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.FINANCIERA;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.OBJECTIVES)) {
            opportunities.add(buildOpportunity(
                "Definir objetivos financieros medibles",
                "Objetivos claros permiten presupuestar, medir avance y atraer financiamiento.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.OBJECTIVES),
                ImpactLevel.HIGH, EffortLevel.LOW,
                List.of("OBJECTIVES_NO_CUBIERTOS"),
                AnalyzedDimension.OBJECTIVES,
                "Dimensión OBJECTIVES no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.RESOURCES)) {
            opportunities.add(buildOpportunity(
                "Estructurar el plan de financiamiento",
                "Definir fuentes y uso de recursos habilita la búsqueda de capital e inversión.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.RESOURCES),
                ImpactLevel.HIGH, EffortLevel.HIGH,
                List.of("RESOURCES_FINANCIEROS_NO_CUBIERTOS"),
                AnalyzedDimension.RESOURCES,
                "Dimensión RESOURCES no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "financi")) {
            opportunities.add(buildOpportunity(
                "Capitalizar la oportunidad financiera detectada",
                "La evaluación detectó una oportunidad financiera que conviene explotar.",
                priorityFromScore, 2, 0,
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("SEÑAL_FINANCIERA_DETECTADA"),
                AnalyzedDimension.OBJECTIVES,
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
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad financiera.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
