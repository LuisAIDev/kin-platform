package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades de escalabilidad (ESCALABILIDAD). Detecta la
 * brecha de escalabilidad del proyecto como oportunidad de crecimiento.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class ScalabilityOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.ESCALABILIDAD;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.SCALABILITY)) {
            opportunities.add(buildOpportunity(
                "Diseñar el plan de escalabilidad",
                "Definir cómo crecerá el proyecto (canales, capacidad, procesos) habilita el escalamiento.",
                priorityFromScore, 0,
                assembler.missingBonus(evaluation, AnalyzedDimension.SCALABILITY),
                ImpactLevel.HIGH, EffortLevel.HIGH,
                List.of("SCALABILITY_NO_CUBIERTA"),
                AnalyzedDimension.SCALABILITY,
                "Dimensión SCALABILITY no cubierta",
                evaluation));
        }

        if (assembler.hasSignal(evaluation, "escal")) {
            opportunities.add(buildOpportunity(
                "Aprovechar la oportunidad de escalar detectada",
                "La evaluación detectó potencial de escalamiento que conviene planificar.",
                priorityFromScore, 2, 0,
                ImpactLevel.HIGH, EffortLevel.HIGH,
                List.of("SEÑAL_ESCALABILIDAD_DETECTADA"),
                AnalyzedDimension.SCALABILITY,
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
        var reason = "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad de escalabilidad.";
        return assembler.build(category(), title, description,
            priorityFromScore, missingBonus, detectedBonus,
            impact, effort, rules, dimension, reason, evidence, evaluation, version());
    }
}
