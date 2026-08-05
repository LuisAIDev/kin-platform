package com.kinplatform.kin.knowledge.citation;

import java.util.List;

/**
 * Resultado inmutable del Citation Engine (Fase 5): bundle de citas + decisiones
 * de cada hecho (auditables). Nadie más modifica el bundle tras su creación.
 */
public record CitationResult(
    CitationBundle bundle,
    List<CitationDecision> decisions
) {

    public CitationResult {
        bundle = bundle == null ? CitationBundle.empty(CitationStyle.INLINE, "") : bundle;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
    }

    public boolean isEmpty() {
        return bundle.isEmpty();
    }

    public static CitationResult empty(CitationStyle style, String explanation) {
        return new CitationResult(CitationBundle.empty(style, explanation), List.of());
    }
}
