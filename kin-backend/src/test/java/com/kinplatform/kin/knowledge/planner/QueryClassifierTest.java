package com.kinplatform.kin.knowledge.planner;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryClassifierTest {

    private final QueryClassifier classifier = new QueryClassifier();

    @Test
    void estable_deberiaClasificarDominioEstable() {
        var classification = classifier.classify(
            new QueryIntent(IntentType.CONOCIMIENTO_ESTABLE, Set.of(), "scrum"));

        assertEquals(QueryDomain.STABLE, classification.domain());
        assertTrue(classification.domains().contains(QueryDomain.STABLE));
        assertEquals(0, classification.priority());
        assertTrue(classification.facets().isEmpty());
    }

    @Test
    void documento_deberiaSerDocumentalLocal() {
        var classification = classifier.classify(
            new QueryIntent(IntentType.DOCUMENTO, Set.of(IntentFacet.DOCUMENTO), "pdf"));

        assertEquals(QueryDomain.DOCUMENTAL, classification.domain());
        assertEquals(10, classification.priority());
    }

    @Test
    void multiplesFacetasExternas_deberiaAgregarDominiosYPrioridad() {
        var classification = classifier.classify(new QueryIntent(IntentType.MERCADO,
            Set.of(IntentFacet.MERCADO, IntentFacet.ESTADISTICA), "café"));

        assertEquals(QueryDomain.MARKET, classification.domain());
        assertTrue(classification.domains().contains(QueryDomain.MARKET));
        assertTrue(classification.domains().contains(QueryDomain.STATISTICAL));
        assertEquals(2, classification.domains().size());
        assertEquals(22, classification.priority());
    }

    @Test
    void tresFacetasExternas_deberiaIncrementarPrioridad() {
        var classification = classifier.classify(new QueryIntent(IntentType.MERCADO,
            Set.of(IntentFacet.MERCADO, IntentFacet.COMPETENCIA, IntentFacet.TENDENCIAS), "t"));

        assertEquals(23, classification.priority());
    }

    @Test
    void intencionGeneral_deberiaSerGeneral() {
        var classification = classifier.classify(new QueryIntent(IntentType.GENERAL, Set.of(), ""));

        assertEquals(QueryClassification.general(), classification);
        assertEquals(QueryDomain.GENERAL, classification.domain());
    }

    @Test
    void intentNulo_deberiaSerGeneral() {
        assertEquals(QueryClassification.general(), classifier.classify(null));
    }

    @Test
    void facetaDesconocida_deberiaCaerEnDominioGeneral() {
        var custom = new QueryClassifier(new FacetDomainMapping(Map.of(
            IntentFacet.MERCADO, QueryDomain.MARKET)), FacetOrder.defaults());

        var classification = custom.classify(
            new QueryIntent(IntentType.COMPETENCIA, Set.of(IntentFacet.COMPETENCIA), "t"));

        assertEquals(QueryDomain.GENERAL, classification.domain());
    }

    @Test
    void tipoPrimario_deberiaSeguirElOrdenDeFacetas() {
        var classification = classifier.classify(new QueryIntent(IntentType.LEGAL,
            Set.of(IntentFacet.MERCADO, IntentFacet.LEGAL), "t"));

        assertEquals(IntentType.LEGAL, classification.type());
        assertEquals(QueryDomain.LEGAL, classification.domain());
    }
}
