package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerModelTest {

    @Test
    void queryIntent_deberiaProtegerNulos() {
        var intent = new QueryIntent(null, null, null);

        assertEquals(IntentType.GENERAL, intent.type());
        assertTrue(intent.facets().isEmpty());
        assertEquals("", intent.topic());
        assertEquals(QueryIntent.general(), intent);
    }

    @Test
    void queryClassification_deberiaProtegerNulos() {
        var classification = new QueryClassification(null, null, Set.of(), null, -1);

        assertEquals(IntentType.GENERAL, classification.type());
        assertEquals(QueryDomain.GENERAL, classification.domain());
        assertTrue(classification.domains().contains(QueryDomain.GENERAL));
        assertTrue(classification.facets().isEmpty());
        assertEquals(0, classification.priority());
    }

    @Test
    void plannedQuery_deberiaProtegerNulos() {
        var query = new PlannedQuery(null, null, null, null, 0, null);

        assertEquals("", query.topic());
        assertTrue(query.keywords().isEmpty());
        assertEquals(ProviderType.WEB_SEARCH, query.providerType());
        assertTrue(query.limit() >= 1);
        assertEquals(KnowledgeRequest.DEFAULT_TIME_WINDOW, query.timeWindow());
    }

    @Test
    void queryPlan_deberiaProtegerNulos() {
        var plan = new QueryPlan(null, null, null, null);

        assertEquals(QueryClassification.general(), plan.classification());
        assertEquals(QueryStrategy.SINGLE, plan.strategy());
        assertTrue(plan.queries().isEmpty());
        assertTrue(plan.reasons().isEmpty());
        assertTrue(plan.isEmpty());
        assertTrue(QueryPlan.empty().isEmpty());
    }

    @Test
    void facetOrder_deberiaResolverPrimaria() {
        var order = FacetOrder.defaults();

        assertEquals(0, order.indexOf(IntentFacet.REGULATORIA));
        assertEquals(-1, order.indexOf(null));
        assertEquals(Optional.of(IntentFacet.REGULATORIA),
            order.primaryOf(Set.of(IntentFacet.MERCADO, IntentFacet.REGULATORIA)));
        assertEquals(Optional.empty(), order.primaryOf(Set.of()));
        assertEquals(Optional.empty(), order.primaryOf(null));
    }

    @Test
    void facetImplication_deberiaResolverImplicaciones() {
        var implication = FacetImplication.defaults();

        assertTrue(implication.impliedBy(IntentFacet.MERCADO).contains(IntentFacet.ESTADISTICA));
        assertTrue(implication.impliedBy(IntentFacet.REGULATORIA).contains(IntentFacet.LEGAL));
        assertTrue(implication.impliedBy(IntentFacet.DOCUMENTO).isEmpty());
        assertTrue(new FacetImplication(null).impliedBy(IntentFacet.MERCADO).isEmpty());
    }

    @Test
    void facetDomainMapping_deberiaResolverDominios() {
        var mapping = FacetDomainMapping.defaults();

        assertEquals(QueryDomain.MARKET, mapping.domainOf(IntentFacet.MERCADO));
        assertEquals(QueryDomain.LEGAL, mapping.domainOf(IntentFacet.REGULATORIA));
        assertEquals(QueryDomain.DOCUMENTAL, mapping.domainOf(IntentFacet.RAG));
        assertEquals(QueryDomain.STATISTICAL, mapping.domainOf(IntentFacet.ESTADISTICA));
        assertEquals(QueryDomain.GENERAL, mapping.domainOf(null));
    }

    @Test
    void facetProviderMapping_deberiaResolverTipos() {
        var mapping = FacetProviderMapping.defaults();

        assertEquals(ProviderType.GOVERNMENT, mapping.providerOf(IntentFacet.REGULATORIA));
        assertEquals(ProviderType.GOVERNMENT, mapping.providerOf(IntentFacet.LEGAL));
        assertEquals(ProviderType.STATISTICS, mapping.providerOf(IntentFacet.ESTADISTICA));
        assertEquals(ProviderType.WEB_SEARCH, mapping.providerOf(IntentFacet.TENDENCIAS));
        assertEquals(ProviderType.DOCUMENT, mapping.providerOf(IntentFacet.DOCUMENTO));
        assertEquals(ProviderType.VECTOR_RAG, mapping.providerOf(IntentFacet.RAG));
    }

    @Test
    void facetProviderMapping_desconocido_deberiaSerWebSearch() {
        var mapping = new FacetProviderMapping(null);

        assertEquals(ProviderType.WEB_SEARCH, mapping.providerOf(IntentFacet.COMPETENCIA));
    }

    @Test
    void enums_deberianExponerDisplayName() {
        assertEquals("Regulatoria", IntentType.REGULATORIA.displayName());
        assertEquals("Conocimiento estable", IntentType.CONOCIMIENTO_ESTABLE.displayName());
        assertEquals("General", IntentType.GENERAL.displayName());
        assertEquals("Mercado", IntentFacet.MERCADO.displayName());
        assertEquals("Documento", IntentFacet.DOCUMENTO.displayName());
        assertEquals("Estadístico", QueryDomain.STATISTICAL.displayName());
        assertEquals("Gobierno", ProviderType.GOVERNMENT.displayName());
        assertEquals("Búsqueda web", ProviderType.WEB_SEARCH.displayName());
        assertEquals("RAG vectorial", ProviderType.VECTOR_RAG.displayName());
        assertEquals("Híbrido", QueryStrategy.HYBRID.displayName());
        assertEquals("Secuencial", QueryStrategy.SEQUENTIAL.displayName());
    }

    @Test
    void queryPlan_conQueries_noDebeSerVacio() {
        var plan = new QueryPlan(QueryClassification.general(), QueryStrategy.SINGLE,
            List.of(PlannedQuery.of("t", List.of(), ProviderType.WEB_SEARCH, null, 5,
                Duration.ofDays(1))), List.of("ok"));

        assertTrue(!plan.isEmpty());
        assertEquals(1, plan.queries().size());
    }
}
