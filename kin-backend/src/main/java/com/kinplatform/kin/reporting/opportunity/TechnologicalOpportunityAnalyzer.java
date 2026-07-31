package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades tecnológicas (TECNOLOGICA). Detecta brechas en
 * recursos técnicos que representan oportunidades de adopción de tecnología.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class TechnologicalOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.TECNOLOGICA;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.RESOURCES)) {
            opportunities.add(buildOpportunity(
                "Planificar los recursos tecnológicos",
                "Definir la infraestructura y herramientas técnicas necesarias acelera la ejecución.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.RESOURCES),
                ImpactLevel.MEDIUM, EffortLevel.HIGH,
                List.of("RESOURCES_NO_CUBIERTOS"),
                AnalyzedDimension.RESOURCES,
                "Dimensión RESOURCES no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "tecnolog")) {
            opportunities.add(buildOpportunity(
                "Aprovechar la oportunidad tecnológica detectada",
                "La evaluación detectó una brecha o señal tecnológica que conviene capitalizar.",
                priorityFromScore, 2, 0,
                ImpactLevel.HIGH, EffortLevel.HIGH,
                List.of("SEÑAL_TECNOLOGICA_DETECTADA"),
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
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad tecnológica.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
