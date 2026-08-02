package com.kinplatform.kin.enrichment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Ranking determinista de evidencias para una categoría de análisis (ADR-016).
 * Inmutable.
 *
 * <p>Contiene la lista ordenada (de mayor a menor score de relevancia) de
 * evidencias seleccionadas para una categoría, junto con su confianza agregada
 * (media de los scores). El orden y la agregación son decisiones de Java.</p>
 */
public record EvidenceRank(
    EvidenceCategory category,
    List<KnowledgeEvidence> evidence,
    double confidence
) {

    public EvidenceRank {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public static EvidenceRank of(EvidenceCategory category, List<KnowledgeEvidence> evidence) {
        var sorted = evidence == null ? List.<KnowledgeEvidence>of() : evidence.stream()
            .sorted(Comparator.comparingDouble(KnowledgeEvidence::scoreValue).reversed())
            .toList();
        double conf = sorted.isEmpty() ? 0.0
            : sorted.stream().mapToDouble(KnowledgeEvidence::scoreValue).average().orElse(0.0);
        return new EvidenceRank(category, sorted, conf);
    }

    public int size() {
        return evidence.size();
    }

    public boolean isEmpty() {
        return evidence.isEmpty();
    }

    public Optional<KnowledgeEvidence> top() {
        return evidence.stream().findFirst();
    }
}
