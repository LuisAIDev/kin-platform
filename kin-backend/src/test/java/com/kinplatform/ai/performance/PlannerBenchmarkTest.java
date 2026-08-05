package com.kinplatform.ai.performance;

import com.kinplatform.kin.knowledge.planner.QueryPlanner;
import com.kinplatform.kin.knowledge.planner.QueryStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Benchmark del Query Planner (Fase 8): clasificación, generación de planes y
 * tiempo por estrategia.
 */
class PlannerBenchmarkTest {

    private static final int ITERATIONS = 500;

    @Test
    void benchmarkPorEstrategia() {
        var planner = new QueryPlanner();
        var cases = List.of(
            new Case("Explícame Scrum", QueryStrategy.SINGLE, true),
            new Case("Quiero crear una SAS en Colombia", QueryStrategy.SEQUENTIAL, false),
            new Case("Abrir panadería en Cartagena", QueryStrategy.HYBRID, false),
            new Case("Analiza este PDF", QueryStrategy.LOCAL_ONLY, false),
            new Case("¿Cómo está el mercado del café colombiano?", QueryStrategy.HYBRID, false));

        for (Case testCase : cases) {
            var request = BenchmarkSupport.request(testCase.topic);
            var stats = BenchmarkSupport.measure(() -> planner.plan(request), ITERATIONS);
            System.out.printf("[PERF] planner [%s]: avg=%.3fms p95=%.3fms max=%.3fms%n",
                testCase.strategy, stats.avgMs(), stats.p95Ms(), stats.maxMs());

            var plan = planner.plan(request);
            assertEquals(testCase.strategy, plan.strategy(), "estrategia esperada para: " + testCase.topic);
            if (testCase.stable) {
                assertTrue(plan.isEmpty());
            } else {
                assertTrue(!plan.isEmpty());
            }
        }
    }

    private record Case(String topic, QueryStrategy strategy, boolean stable) {
    }
}
