package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades de innovación (INNOVACION). Detecta brechas en
 * solución, propuesta de valor y MVP que representan oportunidades de
 * diferenciación y validación temprana.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class InnovationOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.INNOVACION;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.SOLUTION)) {
            opportunities.add(buildOpportunity(
                "Definir la solución diferenciadora",
                "Precisar la solución permitirá resaltar el enfoque innovador frente a alternativas existentes.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.SOLUTION),
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("SOLUTION_NO_CUBIERTA"),
                AnalyzedDimension.SOLUTION,
                "Dimensión SOLUTION no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.VALUE_PROPOSITION)) {
            opportunities.add(buildOpportunity(
                "Refinar la propuesta de valor",
                "Una propuesta de valor clara comunica el beneficio diferencial y atrae adoptantes tempranos.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.VALUE_PROPOSITION),
                ImpactLevel.HIGH, EffortLevel.LOW,
                List.of("VALUE_PROPOSITION_NO_CUBIERTA"),
                AnalyzedDimension.VALUE_PROPOSITION,
                "Dimensión VALUE_PROPOSITION no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.MVP)) {
            opportunities.add(buildOpportunity(
                "Diseñar un MVP de validación",
                "Un MVP enfocado permite validar la hipótesis central con el mínimo esfuerzo posible.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.MVP),
                ImpactLevel.HIGH, EffortLevel.HIGH,
                List.of("MVP_NO_DISEÑADO"),
                AnalyzedDimension.MVP,
                "Dimensión MVP no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "innov")) {
            opportunities.add(buildOpportunity(
                "Explotar la señal de innovación detectada",
                "La evaluación detectó una oportunidad de innovación que conviene convertir en ventaja.",
                priorityFromScore, 2, 0,
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("SEÑAL_INNOVACION_DETECTADA"),
                AnalyzedDimension.SOLUTION,
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
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad de innovación.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
