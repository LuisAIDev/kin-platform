package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.knowledge.KnowledgeFact;

import java.util.List;
import java.util.Map;

/**
 * Resultado interno de aplicar una estrategia de deduplicación.
 */
public record StrategyResult(
        List<KnowledgeFact> remaining,
        Map<KnowledgeFact, List<KnowledgeFact>> duplicateGroups,
        int duplicatesFound,
        int duplicatesRemoved
) {
    public StrategyResult {
        remaining = remaining == null ? List.of() : List.copyOf(remaining);
        duplicateGroups = duplicateGroups == null ? Map.of() : Map.copyOf(duplicateGroups);
    }

    public int inputSize() {
        return remaining.size() + duplicateGroups.values().stream().mapToInt(List::size).sum();
    }

    public int duplicatesFound() {
        return duplicateGroups.values().stream().mapToInt(List::size).sum();
    }

    public int duplicatesRemoved() {
        return duplicateGroups.values().stream().mapToInt(List::size).sum();
    }

    public double confidence() {
        // Confianza basada en la estrategia (exact = 1.0, fuzzy = threshold, semantic = threshold)
        return 1.0; // Simplificado, se puede refinar
    }
}