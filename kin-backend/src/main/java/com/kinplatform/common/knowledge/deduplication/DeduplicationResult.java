package com.kinplatform.common.knowledge.deduplication;

import com.kinplatform.common.engine.EngineResult;
import com.kinplatform.common.knowledge.KnowledgeFact;

import java.util.List;
import java.util.Map;

/**
 * Resultado inmutable del motor de deduplicación.
 */
public record DeduplicationResult(
        List<KnowledgeFact> uniqueFacts,
        Map<KnowledgeFact, List<KnowledgeFact>> duplicateGroups,
        DeduplicationMetrics metrics
) implements EngineResult {

    public DeduplicationResult {
        uniqueFacts = uniqueFacts == null ? List.of() : List.copyOf(uniqueFacts);
        duplicateGroups = duplicateGroups == null ? Map.of() : Map.copyOf(duplicateGroups);
        metrics = metrics == null ? new DeduplicationMetrics(0, 0, 0, 0, List.of()) : metrics;
    }

    @Override
    public double confidence() {
        return metrics.confidence();
    }

    @Override
    public String explanation() {
        return metrics.explanation();
    }

    @Override
    public String generatedBy() {
        return "DeduplicationEngine";
    }

    @Override
    public String engineVersion() {
        return "v1";
    }

    @Override
    public boolean isEmpty() {
        return uniqueFacts.isEmpty();
    }

    public static DeduplicationResult empty() {
        return new DeduplicationResult(
                List.of(),
                Map.of(),
                new DeduplicationMetrics(0, 0, 0, 1.0, List.of())
        );
    }
}


