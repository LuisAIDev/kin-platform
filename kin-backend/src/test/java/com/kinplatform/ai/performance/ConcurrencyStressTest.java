package com.kinplatform.ai.performance;

import com.kinplatform.ai.observability.CorrelationContext;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test del Knowledge Engine (Fase 8): 100/500/1000/5000 requests
 * concurrentes, thread-safety y aislamiento del CorrelationContext (ThreadLocal).
 */
class ConcurrencyStressTest {

    @Test
    void requestsConcurrentes_deberianSerCorrectosYDeterministas() {
        int[] counts = {100, 500, 1000, 5000};
        var runtime = BenchmarkSupport.runtime(null);

        for (int count : counts) {
            long start = System.nanoTime();
            var results = runConcurrent(count, () -> runtime.acquire(
                BenchmarkSupport.request("Mercado del café colombiano")).isEmpty());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

            long nonEmpty = results.stream().filter(v -> !v).count();
            System.out.printf("[PERF] concurrentes=%d: elapsed=%dms, no-vacíos=%d/%d%n",
                count, elapsedMs, nonEmpty, count);

            assertEquals(count, results.size());
            assertEquals(count, nonEmpty, "todos los requests concurrentes deben producir resultados");
        }
    }

    @Test
    void determinismo_bajoConcurrencia() {
        var runtime = BenchmarkSupport.runtime(null);
        var request = BenchmarkSupport.request("Abrir panadería en Cartagena");

        var results = runConcurrent(500, () -> runtime.acquire(request).factCount());

        long distinct = results.stream().distinct().count();
        assertEquals(1, distinct, "misma entrada debe producir el mismo resultado bajo concurrencia");
    }

    @Test
    void correlationContext_deberiaAislarsePorHilo() {
        var futures = new ArrayList<Future<String>>();
        var executor = Executors.newFixedThreadPool(4);
        try {
            for (int i = 0; i < 8; i++) {
                final int id = i;
                futures.add(executor.submit(() -> {
                    CorrelationContext.set(new CorrelationContext.Correlation("c-" + id, "r-" + id, "t-" + id));
                    String current = CorrelationContext.current().correlationId();
                    CorrelationContext.clear();
                    return current;
                }));
            }
            for (int i = 0; i < futures.size(); i++) {
                assertEquals("c-" + i, futures.get(i).get());
            }
        } catch (Exception ex) {
            throw new AssertionError("error en el stress del ThreadLocal", ex);
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> List<T> runConcurrent(int count, Callable<T> task) {
        var executor = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<T>>(count);
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(task));
            }
            var results = new ArrayList<T>(count);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (Exception ex) {
            throw new AssertionError("error en stress test", ex);
        } finally {
            executor.shutdownNow();
        }
    }
}
