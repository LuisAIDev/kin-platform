package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades de automatización (AUTOMATIZACION). Detecta
 * brechas en recursos y objetivos que representan oportunidades de automatizar
 * procesos.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class AutomationOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.AUTOMATIZACION;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.RESOURCES)) {
            opportunities.add(buildOpportunity(
                "Automatizar procesos operativos",
                "Identificar tareas repetitivas para automatizar libera recursos y acelera la operación.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.RESOURCES),
                ImpactLevel.MEDIUM, EffortLevel.HIGH,
                List.of("AUTOMATIZACION_NO_CUBIERTA"),
                AnalyzedDimension.RESOURCES,
                "Dimensión RESOURCES no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.OBJECTIVES)) {
            opportunities.add(buildOpportunity(
                "Digitalizar el seguimiento de objetivos",
                "Automatizar el seguimiento de objetivos mejora la trazabilidad y reduce fricción operativa.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.OBJECTIVES),
                ImpactLevel.MEDIUM, EffortLevel.MEDIUM,
                List.of("OBJECTIVES_AUTOMATIZABLES"),
                AnalyzedDimension.OBJECTIVES,
                "Dimensión OBJECTIVES no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "automat")) {
            opportunities.add(buildOpportunity(
                "Implementar la automatización detectada",
                "La evaluación detectó una oportunidad de automatización que conviene implementar.",
                priorityFromScore, 2, 0,
                ImpactLevel.HIGH, EffortLevel.HIGH,
                List.of("SEÑAL_AUTOMATIZACION_DETECTADA"),
                AnalyzedDimension.RESOURCES,
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
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad de automatización.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
