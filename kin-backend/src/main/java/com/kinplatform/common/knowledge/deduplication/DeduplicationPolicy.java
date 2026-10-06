package com.kinplatform.common.knowledge.deduplication;

import java.util.List;

/**
 * Política de deduplicación que define qué estrategias aplicar y en qué orden.
 */
public enum DeduplicationPolicy {
    /**
     * Solo coincidencia exacta (fuente, tipo, dimensiones, valor).
     */
    EXACT_ONLY(List.of("EXACT_MATCH")),

    /**
     * Exacta primero, luego fuzzy para textos con umbral configurable.
     */
    EXACT_THEN_FUZZY(List.of("EXACT_MATCH", "FUZZY_MATCH")),

    /**
     * Exacta, fuzzy y semántica (para embeddings futuros).
     */
    FULL(List.of("EXACT_MATCH", "FUZZY_MATCH", "SEMANTIC"));

    private final List<String> strategyNames;

    DeduplicationPolicy(List<String> strategyNames) {
        this.strategyNames = List.copyOf(strategyNames);
    }

    public List<String> getStrategyNames() {
        return strategyNames;
    }

    public static DeduplicationPolicy fromString(String value) {
        if (value == null || value.isBlank()) {
            return EXACT_THEN_FUZZY;
        }
        return switch (value.toUpperCase().trim()) {
            case "EXACT_ONLY" -> EXACT_ONLY;
            case "EXACT_THEN_FUZZY" -> EXACT_THEN_FUZZY;
            case "FULL" -> FULL;
            default -> EXACT_THEN_FUZZY;
        };
    }
}
