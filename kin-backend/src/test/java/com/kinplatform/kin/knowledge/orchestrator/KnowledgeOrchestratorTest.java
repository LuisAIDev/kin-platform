package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;
import com.kinplatform.kin.knowledge.planner.ProviderType;
import com.kinplatform.kin.knowledge.planner.QueryStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.ASSEMBLING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.CACHE_LOOKUP;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.COMPLETED;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.FAILED;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.FETCHING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.IDLE;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.PLANNING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.PROVIDER_SELECTION;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.RANKING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.VALIDATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeOrchestratorTest {

    private final KnowledgeOrchestrator orchestrator = new KnowledgeOrchestrator();

    private OrchestrationRequest request(String topic) {
        return OrchestrationRequest.of(KnowledgeRequest.of(topic, List.of()),
            PolicyConfig.defaults(), OrchestrationStrategy.GRACEFUL_DEGRADATION,
            ExecutionEnvironment.online());
    }

    @Test
    void scrum_noDebeConsultarYFinalizarInmediatamente() {
        var result = orchestrator.coordinate(request("Explícame Scrum"));

        assertTrue(result.completed());
        assertTrue(result.plan().isEmpty());
        assertFalse(result.consulted());
        assertEquals(List.of(IDLE, PLANNING, COMPLETED), result.statesVisited());
    }

    @Test
    void analizaEstePdf_deberiaSerLocalFirstSinProveedoresWeb() {
        var result = orchestrator.coordinate(request("Analiza este PDF"));

        assertTrue(result.completed());
        assertEquals(OrchestrationStrategy.LOCAL_FIRST, result.plan().strategy());
        assertEquals(List.of(ProviderType.DOCUMENT), result.selectedProviderTypes());
        assertFalse(result.plan().queries().stream()
            .anyMatch(query -> query.providerType() == ProviderType.WEB_SEARCH));
        assertFalse(result.degraded());
    }

    @Test
    void mercadoDelCafe_deberiaSerHybridConProveedorAbstracto() {
        var result = orchestrator.coordinate(request("¿Cómo está el mercado del café colombiano?"));

        assertTrue(result.completed());
        assertEquals(OrchestrationStrategy.HYBRID, result.plan().strategy());
        assertTrue(result.consulted());
        assertTrue(result.selectedProviderTypes().contains(ProviderType.STATISTICS));
        assertEquals(List.of(IDLE, PLANNING, CACHE_LOOKUP, PROVIDER_SELECTION, FETCHING,
            VALIDATION, RANKING, ASSEMBLING, COMPLETED), result.statesVisited());
    }

    @Test
    void crearSas_deberiaSerSecuencialConProveedorGobierno() {
        var result = orchestrator.coordinate(request("Quiero crear una SAS en Colombia"));

        assertTrue(result.completed());
        assertEquals(QueryStrategy.SEQUENTIAL, result.plan().queryPlan().strategy());
        assertTrue(result.selectedProviderTypes().contains(ProviderType.GOVERNMENT));
    }

    @Test
    void panaderiaCartagena_deberiaSerHybridConMultiplesFacetas() {
        var result = orchestrator.coordinate(request("Abrir panadería en Cartagena"));

        assertTrue(result.completed());
        assertEquals(OrchestrationStrategy.HYBRID, result.plan().strategy());
        assertTrue(result.plan().queryPlan().classification().facets().size() >= 4);
        assertTrue(result.selectedProviderTypes().size() >= 2);
        assertTrue(result.selectedProviderTypes().contains(ProviderType.GOVERNMENT));
        assertTrue(result.selectedProviderTypes().contains(ProviderType.STATISTICS));
    }

    @Test
    void determinismo_mismaEntradaMismoResultado() {
        var request = request("Abrir panadería en Cartagena");

        var result1 = orchestrator.coordinate(request);
        var result2 = orchestrator.coordinate(request);

        assertEquals(result1, result2);
    }

    @Test
    void requestNulo_deberiaFallarSinExcepcion() {
        var result = orchestrator.coordinate(null);

        assertTrue(result.failed());
        assertEquals(FAILED, result.finalState());
        assertTrue(result.failureReason().contains("Solicitud inválida"));
    }

    @Test
    void requestVacio_deberiaFallar() {
        var result = orchestrator.coordinate(request("   "));

        assertTrue(result.failed());
    }
}
