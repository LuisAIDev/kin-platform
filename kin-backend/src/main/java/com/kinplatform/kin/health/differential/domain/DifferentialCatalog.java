package com.kinplatform.kin.health.differential.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catálogo inmutable de conocimiento del diagnóstico diferencial (ADR-029).
 *
 * <p>Snapshot de factores de riesgo y pruebas recomendadas indexados por
 * condición. Se carga desde el puerto
 * {@code DifferentialKnowledgeRepository}; los índices permiten resolución
 * O(1) sin tocar la infraestructura.</p>
 */
public record DifferentialCatalog(
        List<RiskFactor> riskFactors,
        List<RecommendedTest> recommendedTests,
        Map<UUID, List<RiskFactor>> riskFactorsByCondition,
        Map<UUID, List<RecommendedTest>> testsByCondition) {

    public DifferentialCatalog {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        recommendedTests = recommendedTests == null ? List.of() : List.copyOf(recommendedTests);
        riskFactorsByCondition = buildRiskByCondition(riskFactors);
        testsByCondition = buildTestsByCondition(recommendedTests);
    }

    /**
     * Constructor de conveniencia (índices derivados de las listas).
     */
    public DifferentialCatalog(List<RiskFactor> riskFactors, List<RecommendedTest> recommendedTests) {
        this(riskFactors, recommendedTests, Map.of(), Map.of());
    }

    public static DifferentialCatalog empty() {
        return new DifferentialCatalog(List.of(), List.of());
    }

    public boolean isEmpty() {
        return riskFactors.isEmpty() && recommendedTests.isEmpty();
    }

    public List<RiskFactor> riskFactorsFor(UUID conditionId) {
        return riskFactorsByCondition.getOrDefault(conditionId, List.of());
    }

    public List<RecommendedTest> recommendedTestsFor(UUID conditionId) {
        return testsByCondition.getOrDefault(conditionId, List.of());
    }

    private static Map<UUID, List<RiskFactor>> buildRiskByCondition(List<RiskFactor> riskFactors) {
        return riskFactors.stream()
                .collect(Collectors.groupingBy(
                        RiskFactor::conditionId, Collectors.collectingAndThen(Collectors.toList(), List::copyOf)));
    }

    private static Map<UUID, List<RecommendedTest>> buildTestsByCondition(List<RecommendedTest> tests) {
        return tests.stream()
                .collect(Collectors.groupingBy(
                        RecommendedTest::conditionId, Collectors.collectingAndThen(Collectors.toList(), List::copyOf)));
    }
}
