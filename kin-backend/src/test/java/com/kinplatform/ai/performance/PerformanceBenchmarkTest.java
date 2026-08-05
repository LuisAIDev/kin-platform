package com.kinplatform.ai.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Benchmark del ciclo completo del Knowledge Engine (Fase 8): throughput,
 * tiempo promedio, P95, P99 y máximo. Solo mide; no modifica el dominio.
 */
class PerformanceBenchmarkTest {

    private static final int ITERATIONS = 300;

    @Test
    void benchmarkCicloCompleto() {
        var runtime = BenchmarkSupport.runtime(null);
        var request = BenchmarkSupport.request("Mercado del café colombiano");

        var stats = BenchmarkSupport.measure(() -> runtime.acquire(request), ITERATIONS);

        System.out.printf("[PERF] ciclo completo: avg=%.3fms p95=%.3fms p99=%.3fms max=%.3fms throughput=%.0f ops/s%n",
            stats.avgMs(), stats.p95Ms(), stats.p99Ms(), stats.maxMs(), stats.throughputOpsPerSec());

        var result = runtime.acquire(request);
        assertFalse(result.isEmpty());
    }
}
