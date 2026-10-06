package com.kinplatform.common.knowledge.deduplication;

/**
 * Métrica por estrategia de deduplicación.
 */
public record StrategyMetric(
        String strategyName,
        int inputFacts,
        int duplicatesFound,
        int duplicatesRemoved,
        double confidence
) {
    public StrategyMetric {
        if (strategyName == null || strategyName.isBlank()) {
            throw new IllegalArgumentException("strategyName cannot be null or blank");
        }
    }
}
