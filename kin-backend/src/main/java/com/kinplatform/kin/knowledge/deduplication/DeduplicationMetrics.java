package com.kinplatform.kin.knowledge.deduplication;

import java.util.List;

/**
 * Métricas del proceso de deduplicación.
 */
public record DeduplicationMetrics(
        int totalInputFacts,
        int uniqueFacts,
        int duplicatesRemoved,
        double confidence,
        List<StrategyMetric> byStrategy
) {
    public DeduplicationMetrics {
        byStrategy = byStrategy == null ? List.of() : List.copyOf(byStrategy);
    }

    public String explanation() {
        if (totalInputFacts == 0) {
            return "No candidates to deduplicate";
        }
        return String.format(
                "Deduplicación completada: %d hechos de entrada → %d únicos (%d duplicados eliminados). Confianza: %.2f",
                totalInputFacts, uniqueFacts, duplicatesRemoved, confidence);
    }

    public static DeduplicationMetrics empty() {
        return new DeduplicationMetrics(0, 0, 0, 1.0, List.of());
    }
}