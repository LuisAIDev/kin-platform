package com.kinplatform.ai.performance;

import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresión de rendimiento (Fase 8): compara el gateway plano (sin
 * observabilidad) contra el runtime observado. Documenta la diferencia de
 * sobrecarga y verifica compatibilidad de resultados.
 */
class PerformanceRegressionTest {

    private static final int ITERATIONS = 300;

    @Test
    void comparacionObservabilidad_sobrecargaYCompatibilidad() {
        var registry = BenchmarkSupport.registry();
        var request = BenchmarkSupport.request("Mercado del café colombiano");

        var plain = new KnowledgeGateway(registry, BenchmarkSupport.validator());
        var plainStats = BenchmarkSupport.measure(() -> plain.acquire(request), ITERATIONS);

        var observable = BenchmarkSupport.runtime(registry, null);
        var observableStats = BenchmarkSupport.measure(() -> observable.acquire(request), ITERATIONS);

        double overheadFactor = plainStats.avgMs() > 0 ? observableStats.avgMs() / plainStats.avgMs() : 0;
        System.out.printf("[PERF] regresión: plain avg=%.3fms, observable avg=%.3fms, factor=%.2fx%n",
            plainStats.avgMs(), observableStats.avgMs(), overheadFactor);

        assertEquals(plain.acquire(request), observable.acquire(request),
            "el runtime observado debe producir el mismo KnowledgeResult que el gateway");

        assertTrue(observableStats.avgMs() < Math.max(500, plainStats.avgMs() * 50),
            "la sobrecarga de observabilidad debe ser acotada");
    }
}
