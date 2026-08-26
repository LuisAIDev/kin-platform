package com.kinplatform.kin.health.differential.domain;

import java.util.List;

/**
 * Actualización masiva del catálogo diferencial (ADR-029).
 *
 * <p>Factores de riesgo y pruebas recomendadas nuevos/actualizados que un
 * {@code KnowledgeSource} médico aporta para ampliar el diagnóstico diferencial.
 * La infraestructura decide cómo persistirla (upsert idempotente).</p>
 */
public record CatalogUpdate(String source, List<RiskFactor> riskFactors, List<RecommendedTest> recommendedTests) {

    public CatalogUpdate {
        source = source == null ? "" : source;
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        recommendedTests = recommendedTests == null ? List.of() : List.copyOf(recommendedTests);
    }

    public boolean isEmpty() {
        return riskFactors.isEmpty() && recommendedTests.isEmpty();
    }

    public static CatalogUpdate empty() {
        return new CatalogUpdate("", List.of(), List.of());
    }

    public static CatalogUpdate of(
            String source, List<RiskFactor> riskFactors, List<RecommendedTest> recommendedTests) {
        return new CatalogUpdate(source, riskFactors, recommendedTests);
    }
}
