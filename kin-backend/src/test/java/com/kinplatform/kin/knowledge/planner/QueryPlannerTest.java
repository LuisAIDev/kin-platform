package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPlannerTest {

    private final QueryPlanner planner = new QueryPlanner();

    @Test
    void scrum_deberiaClasificarComoConocimientoEstable() {
        var plan = planner.plan(KnowledgeRequest.of("Explícame Scrum", List.of()));

        assertEquals(IntentType.CONOCIMIENTO_ESTABLE, plan.classification().type());
        assertEquals(QueryStrategy.SINGLE, plan.strategy());
        assertTrue(plan.isEmpty());
    }

    @Test
    void crearSas_deberiaProducirRegulatoriaYLegal() {
        var plan = planner.plan(KnowledgeRequest.of("Quiero crear una SAS en Colombia", List.of()));

        assertTrue(plan.classification().facets().contains(IntentFacet.REGULATORIA));
        assertTrue(plan.classification().facets().contains(IntentFacet.LEGAL));
        assertEquals(QueryStrategy.SEQUENTIAL, plan.strategy());
    }

    @Test
    void panaderia_deberiaProducirMultiplesFacetas() {
        var plan = planner.plan(KnowledgeRequest.of("Abrir panadería en Cartagena", List.of()));

        assertTrue(plan.classification().facets().size() >= 3);
        assertFalse(plan.isEmpty());
        assertEquals(QueryStrategy.HYBRID, plan.strategy());
    }

    @Test
    void analizaEstePdf_deberiaSerDocumentoLocalOnly() {
        var plan = planner.plan(KnowledgeRequest.of("Analiza este PDF", List.of()));

        assertEquals(IntentType.DOCUMENTO, plan.classification().type());
        assertEquals(QueryStrategy.LOCAL_ONLY, plan.strategy());
        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.DOCUMENT, plan.queries().get(0).providerType());
    }

    @Test
    void mercadoDelCafe_deberiaSerMercadoEstadisticaHybrid() {
        var plan = planner.plan(KnowledgeRequest.of("¿Cómo está el mercado del café colombiano?", List.of()));

        assertTrue(plan.classification().facets().contains(IntentFacet.MERCADO));
        assertTrue(plan.classification().facets().contains(IntentFacet.ESTADISTICA));
        assertEquals(QueryStrategy.HYBRID, plan.strategy());
    }

    @Test
    void determinismo_mismaEntradaMismoPlan() {
        var request = KnowledgeRequest.of("Abrir panadería en Cartagena", List.of());

        var plan1 = planner.plan(request);
        var plan2 = planner.plan(request);

        assertEquals(plan1, plan2);
    }

    @Test
    void requestNulo_deberiaDevolverPlanSeguro() {
        var plan = planner.plan(null);

        assertNotNull(plan);
        assertEquals(QueryClassification.general(), plan.classification());
    }

    @Test
    void requestVacio_deberiaGenerarConsultaWebDeRespaldo() {
        var plan = planner.plan(KnowledgeRequest.of("", List.of()));

        assertEquals(IntentType.GENERAL, plan.classification().type());
        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.WEB_SEARCH, plan.queries().get(0).providerType());
    }
}
