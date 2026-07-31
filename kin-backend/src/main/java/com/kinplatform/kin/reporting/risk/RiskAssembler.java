package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;

import java.util.List;

/**
 * Ensamblador compartido de {@link Risk}: construye la explicación auditable
 * (información utilizada, regla aplicada, motivo y evidencia) y la confianza
 * determinista a partir de la evaluación de completitud.
 *
 * <p>Elimina la duplicación entre los cuatro {@link RiskAnalyzer}: todos
 * aplican la misma fórmula de confianza y el mismo formato de explicación;
 * solo varían los datos específicos de su categoría. Servicio de dominio puro,
 * stateless y determinista.</p>
 */
public final class RiskAssembler {

    public Risk build(RiskCategory category, String title, String description,
                      RiskLevel severity, RiskLevel probability, RiskLevel impact,
                      List<String> rules, AnalyzedDimension dimension, String reason,
                      String evidence, CompletenessEvaluation evaluation, String version) {
        var explanation = RiskExplanation.of(
            List.of(
                "Cobertura del proyecto: " + Math.round(evaluation.coveragePercent() * 100) + "%",
                "Dimensiones cubiertas: " + evaluation.dimensionsCovered() + "/" + evaluation.totalDimensions()
            ),
            rules.get(0),
            reason,
            evidence
        );
        return Risk.create(category, title, description, severity, probability, impact,
            computeConfidence(evaluation), explanation, rules, dimension, version);
    }

    private double computeConfidence(CompletenessEvaluation evaluation) {
        double raw = 0.35 + 0.35 * evaluation.coveragePercent() + 0.3 * evaluation.qualityOfInformation();
        return Math.max(0.0, Math.min(1.0, raw));
    }
}
