package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.List;

/**
 * Ensamblador compartido de {@link Opportunity}: construye la explicación
 * auditable (información utilizada, regla aplicada, motivo y evidencia), la
 * confianza determinista y la prioridad a partir del score y las señales de
 * completitud.
 *
 * <p>Elimina la duplicación entre los analizadores de oportunidad: todos
 * aplican las mismas fórmulas de confianza y prioridad y el mismo formato de
 * explicación; solo varían los datos específicos de su categoría. Servicio de
 * dominio puro, stateless y determinista.</p>
 */
public final class OpportunityAssembler {

    /**
     * Construye una {@link Opportunity} con confianza, prioridad y explicación deterministas.
     *
     * @param priorityFromScore base de prioridad derivada del score global (0..5)
     */
    public Opportunity build(OpportunityCategory category, String title, String description,
                             int priorityFromScore, int missingBonus, int detectedBonus,
                             ImpactLevel impactLevel, EffortLevel effortLevel,
                             List<String> rules, AnalyzedDimension dimension, String reason,
                             String evidence, CompletenessEvaluation evaluation, String version) {
        var explanation = OpportunityExplanation.of(
            List.of(
                "Cobertura del proyecto: " + Math.round(evaluation.coveragePercent() * 100) + "%",
                "Dimensiones cubiertas: " + evaluation.dimensionsCovered() + "/" + evaluation.totalDimensions()
            ),
            rules.get(0),
            reason,
            evidence
        );
        int priority = clampPriority(priorityFromScore + missingBonus + detectedBonus);
        return Opportunity.create(category, title, description, priority, impactLevel, effortLevel,
            computeConfidence(evaluation), explanation, rules, dimension, version);
    }

    public int computePriorityFromScore(int totalScore) {
        return (int) Math.round((100 - totalScore) / 20.0);
    }

    public int missingBonus(CompletenessEvaluation evaluation, AnalyzedDimension dimension) {
        if (evaluation.criticalMissingDimensions().contains(dimension)) {
            return 3;
        }
        return 2;
    }

    public boolean hasSignal(CompletenessEvaluation evaluation, String keyword) {
        return evaluation.detectedOpportunities().stream()
            .anyMatch(signal -> signal.toLowerCase().contains(keyword));
    }

    private int clampPriority(int priority) {
        return Math.max(1, Math.min(10, priority));
    }

    private double computeConfidence(CompletenessEvaluation evaluation) {
        double raw = 0.35 + 0.35 * evaluation.coveragePercent() + 0.3 * evaluation.qualityOfInformation();
        return Math.max(0.0, Math.min(1.0, raw));
    }
}
