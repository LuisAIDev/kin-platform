package com.kinplatform.kin.knowledge.planner;

import java.util.Map;

/**
 * Mapeo determinista faceta → tipo de proveedor abstracto (especificación
 * Fase 3). Dato declarativo registrable; el generador del plan lo usa. Nunca
 * nombra proveedores concretos (eso pertenece al {@code ProviderRegistry}).
 */
public record FacetProviderMapping(
    Map<IntentFacet, ProviderType> mapping
) {

    public FacetProviderMapping {
        mapping = mapping == null ? Map.of() : Map.copyOf(mapping);
    }

    public static FacetProviderMapping defaults() {
        return new FacetProviderMapping(Map.ofEntries(
            Map.entry(IntentFacet.REGULATORIA, ProviderType.GOVERNMENT),
            Map.entry(IntentFacet.LEGAL, ProviderType.GOVERNMENT),
            Map.entry(IntentFacet.FINANCIERA, ProviderType.STATISTICS),
            Map.entry(IntentFacet.MERCADO, ProviderType.STATISTICS),
            Map.entry(IntentFacet.ESTADISTICA, ProviderType.STATISTICS),
            Map.entry(IntentFacet.COMPETENCIA, ProviderType.WEB_SEARCH),
            Map.entry(IntentFacet.TENDENCIAS, ProviderType.WEB_SEARCH),
            Map.entry(IntentFacet.TECNICA, ProviderType.WEB_SEARCH),
            Map.entry(IntentFacet.ACADEMICA, ProviderType.WEB_SEARCH),
            Map.entry(IntentFacet.DOCUMENTO, ProviderType.DOCUMENT),
            Map.entry(IntentFacet.RAG, ProviderType.VECTOR_RAG)));
    }

    public ProviderType providerOf(IntentFacet facet) {
        if (facet == null) {
            return ProviderType.WEB_SEARCH;
        }
        return mapping.getOrDefault(facet, ProviderType.WEB_SEARCH);
    }
}
