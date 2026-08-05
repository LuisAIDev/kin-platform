package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Cuarto paso del pipeline del Query Planner (especificación Fase 3): produce el
 * {@link QueryPlan} a partir de la clasificación, la estrategia y el request.
 *
 * <p>Genera una {@link PlannedQuery} por faceta (deduplicada por tipo de
 * proveedor abstracto, preservando el orden del {@link FacetOrder}); las
 * estrategias LOCALES/INTERNET filtran las facetas; SINGLE conserva solo la
 * primaria; CACHED y conocimiento estable producen un plan sin consultas. El
 * caso general (sin facetas) produce una consulta web de respaldo.</p>
 */
public class PlanGenerator {

    private final FacetProviderMapping providerMapping;
    private final FacetOrder order;

    public PlanGenerator() {
        this(FacetProviderMapping.defaults(), FacetOrder.defaults());
    }

    public PlanGenerator(FacetProviderMapping providerMapping, FacetOrder order) {
        this.providerMapping = providerMapping == null ? FacetProviderMapping.defaults() : providerMapping;
        this.order = order == null ? FacetOrder.defaults() : order;
    }

    public QueryPlan generate(QueryClassification classification, QueryStrategy strategy,
                              KnowledgeRequest request) {
        var safeRequest = request == null ? KnowledgeRequest.empty() : request;
        var safeClassification = classification == null ? QueryClassification.general() : classification;
        if (safeClassification.type() == IntentType.CONOCIMIENTO_ESTABLE) {
            return new QueryPlan(safeClassification, strategy, List.of(),
                List.of("Conocimiento estable; sin consulta externa"));
        }
        if (strategy == QueryStrategy.CACHED) {
            return new QueryPlan(safeClassification, strategy, List.of(),
                List.of("Estrategia de caché; sin consulta externa"));
        }
        var facets = orderedFacets(safeClassification.facets());
        var selected = selectFacets(facets, strategy);
        var queries = new ArrayList<PlannedQuery>();
        var seen = new LinkedHashSet<ProviderType>();
        for (var facet : selected) {
            var provider = providerMapping.providerOf(facet);
            if (seen.add(provider)) {
                queries.add(PlannedQuery.of(safeRequest.topic(), safeRequest.keywords(),
                    provider, facet, safeRequest.limit(), safeRequest.timeWindow()));
            }
        }
        if (queries.isEmpty()) {
            queries.add(PlannedQuery.of(safeRequest.topic(), safeRequest.keywords(),
                ProviderType.WEB_SEARCH, null, safeRequest.limit(), safeRequest.timeWindow()));
        }
        return new QueryPlan(safeClassification, strategy, List.copyOf(queries),
            reasons(strategy, queries));
    }

    private List<IntentFacet> orderedFacets(Set<IntentFacet> facets) {
        var ordered = new ArrayList<IntentFacet>(facets);
        ordered.sort((a, b) -> Integer.compare(order.indexOf(a), order.indexOf(b)));
        return ordered;
    }

    private List<IntentFacet> selectFacets(List<IntentFacet> facets, QueryStrategy strategy) {
        if (strategy == QueryStrategy.SINGLE) {
            return facets.isEmpty() ? List.of() : List.of(facets.get(0));
        }
        if (strategy == QueryStrategy.LOCAL_ONLY) {
            return facets.stream()
                .filter(facet -> facet == IntentFacet.DOCUMENTO || facet == IntentFacet.RAG)
                .toList();
        }
        if (strategy == QueryStrategy.INTERNET_ONLY) {
            return facets.stream()
                .filter(facet -> facet != IntentFacet.DOCUMENTO && facet != IntentFacet.RAG)
                .toList();
        }
        return facets;
    }

    private static List<String> reasons(QueryStrategy strategy, List<PlannedQuery> queries) {
        return List.of("Estrategia: " + strategy.displayName()
            + ". Consultas planificadas: " + queries.size() + ".");
    }
}
