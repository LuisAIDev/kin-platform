package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.engine.DeterministicId;

import java.util.List;
import java.util.UUID;

/**
 * Riesgo identificado y clasificado por un RiskAnalyzer. Value Object inmutable.
 *
 * <p>Contiene la categoría, severidad, probabilidad, impacto, confianza,
 * explicación (con evidencia), reglas aplicadas y la versión del engine
 * que lo produjo.</p>
 */
public record Risk(
    UUID id,
    RiskCategory category,
    String title,
    String description,
    RiskLevel severity,
    RiskLevel probability,
    RiskLevel impact,
    double confidence,
    RiskExplanation explanation,
    List<String> appliedRules,
    AnalyzedDimension relatedDimension,
    String engineVersion
) {

    public Risk {
        appliedRules = appliedRules == null ? List.of() : List.copyOf(appliedRules);
        explanation = explanation == null ? RiskExplanation.of(List.of(), "", "", "") : explanation;
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    /**
     * Factor de severidad numérico para ordenar riesgos: severidad*3 + probabilidad*2 + impacto.
     */
    public int severityScore() {
        return (severity.ordinal() + 1) * 3 + (probability.ordinal() + 1) * 2 + (impact.ordinal() + 1);
    }

    public static Risk create(RiskCategory category, String title, String description,
                              RiskLevel severity, RiskLevel probability, RiskLevel impact,
                              double confidence, RiskExplanation explanation,
                              List<String> appliedRules, AnalyzedDimension relatedDimension,
                              String engineVersion) {
        var id = DeterministicId.from(category.name(), title, description);
        return new Risk(id, category, title, description, severity, probability, impact,
            confidence, explanation, appliedRules, relatedDimension, engineVersion);
    }
}
