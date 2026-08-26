package com.kinplatform.kin.health.differential.domain;

import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.util.List;
import java.util.UUID;

/**
 * Item del diagnóstico diferencial (ADR-029).
 *
 * <p>Para cada condición candidata: condición, probabilidad ajustada por
 * factores de riesgo, severidad/urgencia (heredadas del triaje), factores de
 * riesgo coincidentes, pruebas complementarias recomendadas y una explicación
 * breve (template determinista en Java) de por qué se sugiere.</p>
 */
public record DifferentialItem(
        UUID conditionId,
        String name,
        String description,
        double probability,
        Severity severity,
        Urgency urgency,
        List<String> matchedSymptoms,
        List<RiskFactor> riskFactors,
        List<RecommendedTest> recommendedTests,
        String reasoning) {

    public DifferentialItem {
        probability = Math.max(0.0, Math.min(1.0, probability));
        description = description == null ? "" : description;
        matchedSymptoms = matchedSymptoms == null ? List.of() : List.copyOf(matchedSymptoms);
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        recommendedTests = recommendedTests == null ? List.of() : List.copyOf(recommendedTests);
        reasoning = reasoning == null ? "" : reasoning;
    }
}
