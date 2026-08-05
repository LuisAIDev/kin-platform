package com.kinplatform.kin.knowledge.planner;

import java.util.Map;
import java.util.Set;

/**
 * Implicaciones deterministas entre facetas (especificación Fase 3): una
 * faceta puede implicar otras. P. ej. MERCADO implica ESTADISTICA (el análisis
 * de mercado requiere cifras) y REGULATORIA implica LEGAL. Dato declarativo
 * registrable; el analizador las aplica después de las reglas directas.
 */
public record FacetImplication(
    Map<IntentFacet, Set<IntentFacet>> implications
) {

    public FacetImplication {
        implications = implications == null ? Map.of() : copy(implications);
    }

    public static FacetImplication defaults() {
        return new FacetImplication(Map.of(
            IntentFacet.MERCADO, Set.of(IntentFacet.ESTADISTICA),
            IntentFacet.REGULATORIA, Set.of(IntentFacet.LEGAL)));
    }

    public Set<IntentFacet> impliedBy(IntentFacet facet) {
        if (facet == null) {
            return Set.of();
        }
        return implications.getOrDefault(facet, Set.of());
    }

    private static Map<IntentFacet, Set<IntentFacet>> copy(
        Map<IntentFacet, Set<IntentFacet>> values) {
        var out = new java.util.LinkedHashMap<IntentFacet, Set<IntentFacet>>();
        for (var entry : values.entrySet()) {
            out.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(out);
    }
}
