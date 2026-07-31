package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades competitivas (COMPETITIVA). Detecta brechas en
 * competencia y propuesta de valor que representan oportunidades de
 * diferenciación frente a rivales.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class CompetitiveOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.COMPETITIVA;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.COMPETITION)) {
            opportunities.add(buildOpportunity(
                "Mapear la competencia y hallar espacios vacíos",
                "Analizar a los competidores revela nichos desatendidos y ventajas explotables.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.COMPETITION),
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("COMPETITION_NO_ANALIZADA"),
                AnalyzedDimension.COMPETITION,
                "Dimensión COMPETITION no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.VALUE_PROPOSITION)) {
            opportunities.add(buildOpportunity(
                "Diferenciar la propuesta frente a rivales",
                "Una propuesta de valor diferenciada convierte debilidades del mercado en ventaja competitiva.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.VALUE_PROPOSITION),
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("VALUE_PROPOSITION_COMPETITIVA_NO_CUBIERTA"),
                AnalyzedDimension.VALUE_PROPOSITION,
                "Dimensión VALUE_PROPOSITION no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "compet")) {
            opportunities.add(buildOpportunity(
                "Explotar la ventaja competitiva detectada",
                "La evaluación detectó una oportunidad competitiva que conviene fortalecer.",
                priorityFromScore, 2, 0,
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("SEÑAL_COMPETITIVA_DETECTADA"),
                AnalyzedDimension.COMPETITION,
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
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad competitiva.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
