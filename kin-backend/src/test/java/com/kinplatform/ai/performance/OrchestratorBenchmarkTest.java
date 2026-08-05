package com.kinplatform.ai.performance;

import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.orchestrator.ExecutionEnvironment;
import com.kinplatform.kin.knowledge.orchestrator.KnowledgeOrchestrator;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationRequest;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Benchmark del Knowledge Orchestrator (Fase 8): ciclo de decisiones, estados,
 * degradación, y comparativa cache hit vs miss.
 */
class OrchestratorBenchmarkTest {

    private static final int ITERATIONS = 200;

    private OrchestrationRequest orchestration(OrchestrationStrategy strategy, ExecutionEnvironment env) {
        return OrchestrationRequest.of(
            BenchmarkSupport.request("Mercado del café colombiano"),
            com.kinplatform.kin.knowledge.policy.PolicyConfig.defaults(), strategy, env);
    }

    @Test
    void benchmarkCicloYEstados() {
        var orchestrator = BenchmarkSupport.runtime(null).orchestrator();

        var stats = BenchmarkSupport.measure(
            () -> orchestrator.coordinateWithResult(orchestration(
                OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online())),
            ITERATIONS);

        System.out.printf("[PERF] orchestrator: avg=%.3fms p95=%.3fms max=%.3fms%n",
            stats.avgMs(), stats.p95Ms(), stats.maxMs());

        var result = orchestrator.coordinateWithResult(orchestration(
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online()));
        assertTrue(result.orchestration().statesVisited().size() >= 8);
        assertEquals(OrchestrationStrategy.HYBRID, result.orchestration().plan().strategy());
    }

    @Test
    void benchmarkDegradacionOffline() {
        var orchestrator = BenchmarkSupport.runtime(null).orchestrator();

        var stats = BenchmarkSupport.measure(
            () -> orchestrator.coordinateWithResult(orchestration(
                OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.offline())),
            ITERATIONS);

        System.out.printf("[PERF] orchestrator offline: avg=%.3fms max=%.3fms%n", stats.avgMs(), stats.maxMs());

        var result = orchestrator.coordinateWithResult(orchestration(
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.offline()));
        assertTrue(result.orchestration().degraded());
    }

    @Test
    void cacheHit_deberiaSerMasRapidoYEvitarFetch() {
        var repository = new SingleRepository();
        var runtime = BenchmarkSupport.runtime(repository);
        var orchestrator = runtime.orchestrator();
        var request = orchestration(OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online());

        orchestrator.coordinateWithResult(request);

        var hitStats = BenchmarkSupport.measure(
            () -> orchestrator.coordinateWithResult(request), ITERATIONS);
        var hit = orchestrator.coordinateWithResult(request);

        assertTrue(hit.cacheHit());
        assertFalse(hit.knowledge().isEmpty());
        System.out.printf("[PERF] orchestrator cache HIT: avg=%.3fms%n", hitStats.avgMs());
    }

    private static final class SingleRepository implements KnowledgeRepository {
        private KnowledgeResult stored;

        @Override
        public Optional<KnowledgeResult> find(KnowledgeQuery query) {
            return Optional.ofNullable(stored);
        }

        @Override
        public void save(KnowledgeResult result, Duration ttl) {
            stored = result;
        }
    }
}
