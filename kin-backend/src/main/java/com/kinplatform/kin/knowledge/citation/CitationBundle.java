package com.kinplatform.kin.knowledge.citation;

import java.util.List;

/**
 * Bundle de citas inmutable (Fase 5 — Citation Engine): citas verificadas,
 * referencias formateadas, metadatos, score y explicación deterministas.
 * Es el único contrato que consumirá la construcción del prompt (nunca
 * {@code KnowledgeFact} ni metadatos crudos).
 */
public record CitationBundle(
    CitationStyle style,
    List<CitationEntry> entries,
    List<String> references,
    CitationMetadata metadata,
    double score,
    String explanation
) {

    public CitationBundle {
        style = style == null ? CitationStyle.INLINE : style;
        entries = entries == null ? List.of() : List.copyOf(entries);
        references = references == null ? List.of() : List.copyOf(references);
        metadata = metadata == null ? CitationMetadata.empty() : metadata;
        score = Math.max(0.0, Math.min(1.0, score));
        explanation = explanation == null ? "" : explanation;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public static CitationBundle empty(CitationStyle style, String explanation) {
        return new CitationBundle(style, List.of(), List.of(), CitationMetadata.empty(), 0.0, explanation);
    }
}
