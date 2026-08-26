package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.knowledge.KnowledgeFact;

import java.util.List;
import java.util.UUID;

/**
 * Entrada para el motor de deduplicación.
 */
public record DeduplicationInput(
        UUID projectId,
        List<KnowledgeFact> candidates,
        DeduplicationPolicy policy,
        double fuzzyThreshold
) implements EngineInput {

    public DeduplicationInput {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        policy = policy == null ? DeduplicationPolicy.EXACT_THEN_FUZZY : policy;
        fuzzyThreshold = Math.max(0.0, Math.min(1.0, fuzzyThreshold));
    }

    public static DeduplicationInput of(UUID projectId, List<KnowledgeFact> candidates) {
        return new DeduplicationInput(projectId, candidates, DeduplicationPolicy.EXACT_THEN_FUZZY, 0.85);
    }

    public static DeduplicationInput of(UUID projectId, List<KnowledgeFact> candidates, DeduplicationPolicy policy) {
        return new DeduplicationInput(projectId, candidates, policy, 0.85);
    }

    public static DeduplicationInput of(UUID projectId, List<KnowledgeFact> candidates, DeduplicationPolicy policy, double fuzzyThreshold) {
        return new DeduplicationInput(projectId, candidates, policy, fuzzyThreshold);
    }
}