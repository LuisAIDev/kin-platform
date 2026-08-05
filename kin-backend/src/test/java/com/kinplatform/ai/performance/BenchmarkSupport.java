package com.kinplatform.ai.performance;

import com.kinplatform.ai.observability.ObservableKnowledgeRuntime;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Soporte compartido de los benchmarks del Knowledge Engine (Fase 8). Solo
 * herramientas de medición: no modifica el dominio.
 */
final class BenchmarkSupport {

    private BenchmarkSupport() {
    }

    static SourceValidator validator() {
        return new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json"));
    }

    static KnowledgeSource source() {
        return new StubSource(List.of(new KnowledgeCandidate(
            "Dato verificado de mercado colombiano con contexto. ".repeat(6),
            "src-1", "Fuente", "https://example.com/reporte", OffsetDateTime.now().minusDays(5),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"))));
    }

    static SourceRegistry registry() {
        return new SourceRegistry(List.of(source()));
    }

    static ObservableKnowledgeRuntime runtime(KnowledgeRepository repository) {
        return runtime(registry(), repository);
    }

    static ObservableKnowledgeRuntime runtime(SourceRegistry registry, KnowledgeRepository repository) {
        return ObservableKnowledgeRuntime.create(registry, validator(), repository,
            new SimpleMeterRegistry());
    }

    static KnowledgeRequest request(String topic) {
        return KnowledgeRequest.of(topic, List.of());
    }

    static Stats measure(Runnable runnable, int iterations) {
        List<Long> nanos = new ArrayList<>(iterations);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            runnable.run();
            nanos.add(System.nanoTime() - t0);
        }
        long totalNanos = System.nanoTime() - start;
        double totalMs = totalNanos / 1_000_000.0;
        List<Long> sorted = new ArrayList<>(nanos);
        sorted.sort(Long::compareTo);
        double avgMs = sorted.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double p95 = percentile(sorted, 0.95) / 1_000_000.0;
        double p99 = percentile(sorted, 0.99) / 1_000_000.0;
        double max = sorted.get(sorted.size() - 1) / 1_000_000.0;
        double opsPerSec = iterations / (totalMs / 1000.0);
        return new Stats(avgMs, p95, p99, max, opsPerSec, iterations);
    }

    private static long percentile(List<Long> sorted, double q) {
        int index = Math.min(sorted.size() - 1, (int) Math.round(q * (sorted.size() - 1)));
        return sorted.get(index);
    }

    record Stats(double avgMs, double p95Ms, double p99Ms, double maxMs,
                 double throughputOpsPerSec, int count) {
    }

    private static final class StubSource implements KnowledgeSource {
        private final List<KnowledgeCandidate> candidates;

        private StubSource(List<KnowledgeCandidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            return candidates;
        }
    }
}
