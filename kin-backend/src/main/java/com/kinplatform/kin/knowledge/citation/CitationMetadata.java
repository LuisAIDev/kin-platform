package com.kinplatform.kin.knowledge.citation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metadatos inmutables del bundle de citas (Fase 5 — Citation Engine): conteo,
 * fuentes usadas y confianza máxima/media de las citas verificadas.
 */
public record CitationMetadata(
    int count,
    Set<String> sources,
    double topConfidence,
    double averageConfidence
) {

    public CitationMetadata {
        count = Math.max(0, count);
        sources = sources == null ? Set.of() : Set.copyOf(sources);
        topConfidence = Math.max(0.0, Math.min(1.0, topConfidence));
        averageConfidence = Math.max(0.0, Math.min(1.0, averageConfidence));
    }

    public static CitationMetadata of(List<CitationEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return empty();
        }
        var sources = entries.stream()
            .map(CitationEntry::sourceId)
            .filter(source -> source != null && !source.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        double top = entries.stream().mapToDouble(CitationEntry::confidence).max().orElse(0.0);
        double avg = entries.stream().mapToDouble(CitationEntry::confidence).average().orElse(0.0);
        return new CitationMetadata(entries.size(), Set.copyOf(sources), top, avg);
    }

    public static CitationMetadata empty() {
        return new CitationMetadata(0, Set.of(), 0.0, 0.0);
    }
}
