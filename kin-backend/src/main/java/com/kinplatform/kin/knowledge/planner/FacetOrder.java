package com.kinplatform.kin.knowledge.planner;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Orden determinista de facetas (especificación Fase 3): define la faceta
 * primaria (menor índice) y la prioridad relativa. Dato declarativo
 * registrable; el analizador y el clasificador lo usan para decidir el tipo
 * primario.
 */
public record FacetOrder(
    List<IntentFacet> order
) {

    public FacetOrder {
        order = order == null ? List.of() : List.copyOf(order);
    }

    public static FacetOrder defaults() {
        return new FacetOrder(List.of(
            IntentFacet.REGULATORIA, IntentFacet.LEGAL, IntentFacet.FINANCIERA,
            IntentFacet.MERCADO, IntentFacet.ESTADISTICA, IntentFacet.COMPETENCIA,
            IntentFacet.TENDENCIAS, IntentFacet.ACADEMICA, IntentFacet.TECNICA,
            IntentFacet.DOCUMENTO, IntentFacet.RAG));
    }

    public int indexOf(IntentFacet facet) {
        if (facet == null) {
            return -1;
        }
        return order.indexOf(facet);
    }

    /**
     * Faceta primaria: la de menor índice en el orden entre las dadas; en caso
     * de empate (faceta no registrada), la de menor ordinal. Determinista.
     */
    public Optional<IntentFacet> primaryOf(Set<IntentFacet> facets) {
        if (facets == null || facets.isEmpty()) {
            return Optional.empty();
        }
        return facets.stream().min((a, b) -> {
            int byIndex = Integer.compare(indexOf(a), indexOf(b));
            return byIndex != 0 ? byIndex : Integer.compare(a.ordinal(), b.ordinal());
        });
    }
}
