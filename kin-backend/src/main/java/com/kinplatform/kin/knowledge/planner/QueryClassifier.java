package com.kinplatform.kin.knowledge.planner;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Segundo paso del pipeline del Query Planner (especificación Fase 3): convierte
 * la {@link QueryIntent} en una {@link QueryClassification} con dominio, dominios
 * implicados, tipo, facetas y prioridad determinista.
 *
 * <p>La prioridad es un valor del dominio: {@code 0} para conocimiento estable,
 * {@code 10} para contexto únicamente local (documentos/RAG) y {@code 20 + N}
 * para consultas externas con N facetas.</p>
 */
public class QueryClassifier {

    private final FacetDomainMapping domainMapping;
    private final FacetOrder order;

    public QueryClassifier() {
        this(FacetDomainMapping.defaults(), FacetOrder.defaults());
    }

    public QueryClassifier(FacetDomainMapping domainMapping, FacetOrder order) {
        this.domainMapping = domainMapping == null ? FacetDomainMapping.defaults() : domainMapping;
        this.order = order == null ? FacetOrder.defaults() : order;
    }

    public QueryClassification classify(QueryIntent intent) {
        if (intent == null) {
            return QueryClassification.general();
        }
        if (intent.type() == IntentType.CONOCIMIENTO_ESTABLE) {
            return new QueryClassification(IntentType.CONOCIMIENTO_ESTABLE, QueryDomain.STABLE,
                Set.of(QueryDomain.STABLE), Set.of(), 0);
        }
        if (intent.facets().isEmpty()) {
            return QueryClassification.general();
        }
        var facets = intent.facets();
        var domains = new LinkedHashSet<QueryDomain>();
        for (var facet : facets) {
            domains.add(domainMapping.domainOf(facet));
        }
        IntentFacet primary = order.primaryOf(facets).orElseGet(() -> facets.iterator().next());
        QueryDomain primaryDomain = domainMapping.domainOf(primary);
        int priority = computePriority(intent.type(), facets);
        return new QueryClassification(intent.type(), primaryDomain, Set.copyOf(domains),
            facets, priority);
    }

    private static int computePriority(IntentType type, Set<IntentFacet> facets) {
        if (type == IntentType.CONOCIMIENTO_ESTABLE) {
            return 0;
        }
        boolean localOnly = facets.stream()
            .allMatch(facet -> facet == IntentFacet.DOCUMENTO || facet == IntentFacet.RAG);
        return localOnly ? 10 : 20 + facets.size();
    }
}
