package com.kinplatform.kin.knowledge.planner;

import java.util.Map;

/**
 * Mapeo determinista faceta → dominio de conocimiento (especificación Fase 3).
 * Dato declarativo registrable; el clasificador lo usa para derivar los
 * dominios de una clasificación.
 */
public record FacetDomainMapping(
    Map<IntentFacet, QueryDomain> mapping
) {

    public FacetDomainMapping {
        mapping = mapping == null ? Map.of() : Map.copyOf(mapping);
    }

    public static FacetDomainMapping defaults() {
        return new FacetDomainMapping(Map.ofEntries(
            Map.entry(IntentFacet.REGULATORIA, QueryDomain.LEGAL),
            Map.entry(IntentFacet.LEGAL, QueryDomain.LEGAL),
            Map.entry(IntentFacet.FINANCIERA, QueryDomain.LEGAL),
            Map.entry(IntentFacet.MERCADO, QueryDomain.MARKET),
            Map.entry(IntentFacet.COMPETENCIA, QueryDomain.MARKET),
            Map.entry(IntentFacet.TENDENCIAS, QueryDomain.MARKET),
            Map.entry(IntentFacet.ESTADISTICA, QueryDomain.STATISTICAL),
            Map.entry(IntentFacet.TECNICA, QueryDomain.TECHNICAL),
            Map.entry(IntentFacet.ACADEMICA, QueryDomain.ACADEMIC),
            Map.entry(IntentFacet.DOCUMENTO, QueryDomain.DOCUMENTAL),
            Map.entry(IntentFacet.RAG, QueryDomain.DOCUMENTAL)));
    }

    public QueryDomain domainOf(IntentFacet facet) {
        if (facet == null) {
            return QueryDomain.GENERAL;
        }
        return mapping.getOrDefault(facet, QueryDomain.GENERAL);
    }
}
