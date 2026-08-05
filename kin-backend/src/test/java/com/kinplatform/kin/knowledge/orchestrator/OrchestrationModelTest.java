package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;
import com.kinplatform.kin.knowledge.policy.QueryMode;
import com.kinplatform.kin.knowledge.planner.ProviderType;
import com.kinplatform.kin.knowledge.planner.QueryPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestrationModelTest {

    @Test
    void request_deberiaProtegerNulos() {
        var request = new OrchestrationRequest(null, null, null, null);

        assertEquals(KnowledgeRequest.empty(), request.knowledgeRequest());
        assertEquals(PolicyConfig.defaults(), request.policyConfig());
        assertEquals(OrchestrationStrategy.GRACEFUL_DEGRADATION, request.strategy());
        assertEquals(ExecutionEnvironment.online(), request.environment());
        assertEquals(OrchestrationRequest.empty(), request);
    }

    @Test
    void plan_deberiaProtegerNulos() {
        var plan = new OrchestrationPlan(null, null, null);

        assertEquals(OrchestrationStrategy.GRACEFUL_DEGRADATION, plan.strategy());
        assertEquals(QueryMode.EXTERNAL, plan.mode());
        assertTrue(plan.isEmpty());
        assertEquals(QueryPlan.empty(), plan.queryPlan());
    }

    @Test
    void decision_deberiaProtegerNulos() {
        var decision = new OrchestrationDecision(null, null, null);

        assertEquals(OrchestrationDecisionType.CONSULT_EXTERNAL, decision.type());
        assertEquals(OrchestrationState.PLANNING, decision.state());
        assertEquals("", decision.reason());
    }

    @Test
    void resultado_deberiaProtegerNulosYExponerAccesores() {
        var result = new OrchestrationResult(null, null, null, null, null, false, null);

        assertEquals(OrchestrationState.COMPLETED, result.finalState());
        assertTrue(result.statesVisited().isEmpty());
        assertTrue(result.decisions().isEmpty());
        assertTrue(result.selectedProviderTypes().isEmpty());
        assertEquals("", result.failureReason());
        assertTrue(result.completed());
        assertFalse(result.failed());
        assertFalse(result.consulted());
    }

    @Test
    void resultado_consultado_deberiaDetectarDecisionExterna() {
        var result = new OrchestrationResult(OrchestrationState.COMPLETED, List.of(),
            new OrchestrationPlan(null, null, null),
            List.of(OrchestrationDecision.of(OrchestrationDecisionType.CONSULT_EXTERNAL,
                OrchestrationState.PLANNING, "consulta")), List.of(), false, "");

        assertTrue(result.consulted());
    }

    @Test
    void environment_deberiaExponerSaludDeProveedores() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.STATISTICS, ProviderHealth.DOWN));

        assertFalse(env.isAvailable(ProviderType.STATISTICS));
        assertTrue(env.isAvailable(ProviderType.GOVERNMENT));
        assertEquals(ProviderHealth.AVAILABLE, env.healthOf(null));
        assertEquals(ProviderHealth.AVAILABLE, env.healthOf(ProviderType.WEB_SEARCH));
        assertTrue(ExecutionEnvironment.online().internetAvailable());
        assertTrue(ExecutionEnvironment.online().cacheHealthy());
        assertFalse(ExecutionEnvironment.offline().internetAvailable());
        assertFalse(ExecutionEnvironment.offline().cacheHealthy());
    }

    @Test
    void catalog_deberiaMapearTiposAbstractos() {
        assertEquals("government", ProviderTypeCatalog.sourceType(ProviderType.GOVERNMENT));
        assertEquals("statistics", ProviderTypeCatalog.sourceType(ProviderType.STATISTICS));
        assertEquals("web_search", ProviderTypeCatalog.sourceType(ProviderType.WEB_SEARCH));
        assertEquals("document", ProviderTypeCatalog.sourceType(ProviderType.DOCUMENT));
        assertEquals("vector_rag", ProviderTypeCatalog.sourceType(ProviderType.VECTOR_RAG));
        assertEquals("internal_db", ProviderTypeCatalog.sourceType(ProviderType.INTERNAL_DB));
        assertEquals("", ProviderTypeCatalog.sourceType(null));
    }

    @Test
    void catalog_deberiaResolverInversamente() {
        assertEquals(Optional.of(ProviderType.GOVERNMENT), ProviderTypeCatalog.fromSourceType("government"));
        assertEquals(Optional.of(ProviderType.STATISTICS), ProviderTypeCatalog.fromSourceType("Statistics"));
        assertEquals(Optional.empty(), ProviderTypeCatalog.fromSourceType("desconocido"));
        assertEquals(Optional.empty(), ProviderTypeCatalog.fromSourceType(null));
        assertEquals(Optional.empty(), ProviderTypeCatalog.fromSourceType(" "));
    }

    @Test
    void enums_deberianExponerDisplayName() {
        assertEquals("Cache first", OrchestrationStrategy.CACHE_FIRST.displayName());
        assertEquals("Fail fast", OrchestrationStrategy.FAIL_FAST.displayName());
        assertEquals("Modo offline", OrchestrationStrategy.OFFLINE_MODE.displayName());
        assertEquals("Disponible", ProviderHealth.AVAILABLE.displayName());
        assertEquals("Caído", ProviderHealth.DOWN.displayName());
        assertEquals("Timeout", ProviderHealth.TIMEOUT.displayName());
        assertEquals("No consultar", OrchestrationDecisionType.NO_CONSULT.displayName());
        assertEquals("Degradar", OrchestrationDecisionType.DEGRADE.displayName());
    }

    @Test
    void plan_conQueries_noDebeSerVacio() {
        var plan = new OrchestrationPlan(OrchestrationStrategy.HYBRID, QueryMode.EXTERNAL,
            new QueryPlan(com.kinplatform.kin.knowledge.planner.QueryClassification.general(),
                com.kinplatform.kin.knowledge.planner.QueryStrategy.SINGLE,
                List.of(com.kinplatform.kin.knowledge.planner.PlannedQuery.of("t", List.of(),
                    ProviderType.WEB_SEARCH, null, 5, java.time.Duration.ofDays(1))),
                List.of("ok")));

        assertTrue(!plan.isEmpty());
        assertEquals(1, plan.queries().size());
    }
}
