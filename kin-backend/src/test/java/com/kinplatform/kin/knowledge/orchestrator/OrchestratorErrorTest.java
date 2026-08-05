package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.policy.CostPolicyConfig;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;
import com.kinplatform.kin.knowledge.planner.ProviderType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestratorErrorTest {

    private final KnowledgeOrchestrator orchestrator = new KnowledgeOrchestrator();

    private OrchestrationRequest request(String topic, OrchestrationStrategy strategy,
                                         ExecutionEnvironment environment) {
        return OrchestrationRequest.of(KnowledgeRequest.of(topic, List.of()),
            PolicyConfig.defaults(), strategy, environment);
    }

    @Test
    void proveedorCaido_deberiaDegradarConGracia() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.STATISTICS, ProviderHealth.DOWN));
        var result = orchestrator.coordinate(request(
            "¿Cómo está el mercado del café colombiano?", OrchestrationStrategy.GRACEFUL_DEGRADATION, env));

        assertTrue(result.completed());
        assertTrue(result.degraded());
        assertTrue(result.selectedProviderTypes().isEmpty());
    }

    @Test
    void proveedorCaido_conFailFast_deberiaFallar() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.STATISTICS, ProviderHealth.DOWN));
        var result = orchestrator.coordinate(request(
            "¿Cómo está el mercado del café colombiano?", OrchestrationStrategy.FAIL_FAST, env));

        assertTrue(result.failed());
        assertTrue(result.failureReason().contains("no disponible"));
    }

    @Test
    void timeout_deberiaDegradarConGracia() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.STATISTICS, ProviderHealth.TIMEOUT));
        var result = orchestrator.coordinate(request(
            "¿Cómo está el mercado del café colombiano?", OrchestrationStrategy.GRACEFUL_DEGRADATION, env));

        assertTrue(result.completed());
        assertTrue(result.degraded());
    }

    @Test
    void timeout_conFailFast_deberiaFallar() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.STATISTICS, ProviderHealth.TIMEOUT));
        var result = orchestrator.coordinate(request(
            "¿Cómo está el mercado del café colombiano?", OrchestrationStrategy.FAIL_FAST, env));

        assertTrue(result.failed());
    }

    @Test
    void sinInternet_conExterna_deberiaDegradar() {
        var result = orchestrator.coordinate(request(
            "¿Cómo está el mercado del café colombiano?",
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.offline()));

        assertTrue(result.completed());
        assertTrue(result.degraded());
        assertTrue(result.decisions().stream()
            .anyMatch(decision -> decision.type() == OrchestrationDecisionType.DEGRADE));
    }

    @Test
    void sinInternet_conFailFast_deberiaFallar() {
        var result = orchestrator.coordinate(request(
            "¿Cómo está el mercado del café colombiano?",
            OrchestrationStrategy.FAIL_FAST, ExecutionEnvironment.offline()));

        assertTrue(result.failed());
    }

    @Test
    void pdfSinInternet_deberiaFuncionarLocal() {
        var result = orchestrator.coordinate(request(
            "Analiza este PDF", OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.offline()));

        assertTrue(result.completed());
        assertTrue(!result.degraded());
        assertEquals(List.of(ProviderType.DOCUMENT), result.selectedProviderTypes());
    }

    @Test
    void cacheCorrupta_cacheFirst_deberiaDegradarYContinuar() {
        var env = new ExecutionEnvironment(true, false, Map.of());
        var result = orchestrator.coordinate(request("mercado", OrchestrationStrategy.CACHE_FIRST, env));

        assertTrue(result.completed());
        assertTrue(result.degraded());
        assertTrue(result.consulted());
    }

    @Test
    void cacheSana_cacheFirst_deberiaUsarSoloCache() {
        var result = orchestrator.coordinate(request("mercado", OrchestrationStrategy.CACHE_FIRST,
            ExecutionEnvironment.online()));

        assertTrue(result.completed());
        assertTrue(!result.degraded());
        assertTrue(!result.consulted());
        assertTrue(result.decisions().stream()
            .anyMatch(decision -> decision.type() == OrchestrationDecisionType.CACHE_ONLY));
    }

    @Test
    void presupuestoAgotado_deberiaDegradar() {
        var policies = new PolicyConfig(null, null, null, CostPolicyConfig.testing(), null);
        var result = orchestrator.coordinate(OrchestrationRequest.of(
            KnowledgeRequest.of("¿Cómo está el mercado del café colombiano?", List.of()),
            policies, OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online()));

        assertTrue(result.completed());
        assertTrue(result.degraded());
        assertTrue(result.decisions().stream()
            .anyMatch(decision -> decision.type() == OrchestrationDecisionType.STOP_CONSULTS));
    }

    @Test
    void presupuestoAgotado_conFailFast_deberiaFallar() {
        var policies = new PolicyConfig(null, null, null, CostPolicyConfig.testing(), null);
        var result = orchestrator.coordinate(OrchestrationRequest.of(
            KnowledgeRequest.of("¿Cómo está el mercado del café colombiano?", List.of()),
            policies, OrchestrationStrategy.FAIL_FAST, ExecutionEnvironment.online()));

        assertTrue(result.failed());
    }

    @Test
    void sinProveedoresDisponibles_deberiaDetenerYDegradar() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.GOVERNMENT, ProviderHealth.DOWN,
                ProviderType.STATISTICS, ProviderHealth.DOWN));
        var result = orchestrator.coordinate(request(
            "Abrir panadería en Cartagena", OrchestrationStrategy.GRACEFUL_DEGRADATION, env));

        assertTrue(result.completed());
        assertTrue(result.degraded());
        assertTrue(result.decisions().stream()
            .anyMatch(decision -> decision.type() == OrchestrationDecisionType.STOP));
    }

    @Test
    void degradacionParcial_deberiaContinuarConDisponibles() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(ProviderType.GOVERNMENT, ProviderHealth.DOWN));
        var result = orchestrator.coordinate(request(
            "Abrir panadería en Cartagena", OrchestrationStrategy.GRACEFUL_DEGRADATION, env));

        assertTrue(result.completed());
        assertTrue(result.degraded());
        assertEquals(List.of(ProviderType.STATISTICS), result.selectedProviderTypes());
    }
}
