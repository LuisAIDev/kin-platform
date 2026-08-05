package com.kinplatform.ai.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Perfil de memoria del Knowledge Engine (Fase 8): medición de heap antes y
 * después de una carga intensiva, y verificación de retención acotada.
 */
class MemoryProfileTest {

    private static final int ITERATIONS = 5000;

    @Test
    void retencionDeberiaSerAcotadaBajoCarga() {
        var runtime = BenchmarkSupport.runtime(null);
        var request = BenchmarkSupport.request("Mercado del café colombiano");

        System.gc();
        long before = usedHeap();

        for (int i = 0; i < ITERATIONS; i++) {
            var result = runtime.acquire(request);
            assertTrue(!result.isEmpty());
        }

        System.gc();
        long after = usedHeap();
        long growth = after - before;

        System.out.printf("[PERF] memoria: before=%dMB after=%dMB growth=%dMB (iterations=%d)%n",
            before / 1_048_576L, after / 1_048_576L, growth / 1_048_576L, ITERATIONS);

        assertTrue(growth < 256L * 1024 * 1024,
            "la retención de heap no debe crecer sin límite bajo carga");
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
