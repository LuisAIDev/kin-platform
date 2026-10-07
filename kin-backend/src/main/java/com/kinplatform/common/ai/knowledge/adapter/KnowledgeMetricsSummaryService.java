package com.kinplatform.common.ai.knowledge.adapter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * Resumen agregado de las métricas del Knowledge Engine (ADR-025, Fase 1):
 * éxito/fallo/timeout/rejected/rate-limit por fuente, latencia p50/p95/p99 y
 * ratio de caché hit/miss. Pensado para un solo {@code curl} legible sin hacer
 * grep sobre el output completo de Prometheus.
 */
@Service
public class KnowledgeMetricsSummaryService {

    public record SourceSummary(
            double requests,
            double success,
            double failure,
            double timeout,
            double rejected,
            double rateLimited,
            Double latencyP50Ms,
            Double latencyP95Ms,
            Double latencyP99Ms) {}

    public record CacheSummary(long hits, long misses, Double hitRatio) {}

    public record KnowledgeMetricsSummary(Map<String, SourceSummary> sources, CacheSummary cache, long generatedAt) {}

    private final MeterRegistry registry;

    public KnowledgeMetricsSummaryService(MeterRegistry registry) {
        this.registry = registry;
    }

    public KnowledgeMetricsSummary summarize() {
        Map<String, SourceSummary> bySource = new TreeMap<>();
        for (Counter counter : registry.find("kin.knowledge.adapter.requests").counters()) {
            String source = counter.getId().getTag("source");
            if (source == null) {
                continue;
            }
            bySource.put(
                    source,
                    new SourceSummary(
                            counter.count(),
                            count("kin.knowledge.adapter.success", source),
                            count("kin.knowledge.adapter.failure", source),
                            count("kin.knowledge.adapter.timeout", source),
                            count("kin.knowledge.adapter.rejected", source),
                            count("kin.knowledge.adapter.rate_limited", source),
                            percentile("kin.knowledge.adapter.latency", source, 0.5),
                            percentile("kin.knowledge.adapter.latency", source, 0.95),
                            percentile("kin.knowledge.adapter.latency", source, 0.99)));
        }
        long hits = (long) cacheCount("kin.knowledge.adapter.cache.hit");
        long misses = (long) cacheCount("kin.knowledge.adapter.cache.miss");
        Double ratio = hits + misses > 0 ? (double) hits / (hits + misses) : null;
        return new KnowledgeMetricsSummary(bySource, new CacheSummary(hits, misses, ratio), System.currentTimeMillis());
    }

    private double count(String name, String source) {
        Counter counter = registry.find(name).tag("source", source).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double cacheCount(String name) {
        Counter counter = registry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private Double percentile(String name, String source, double percentile) {
        Timer timer = registry.find(name).tag("source", source).timer();
        if (timer == null) {
            return null;
        }
        for (ValueAtPercentile value : timer.takeSnapshot().percentileValues()) {
            if (value.percentile() == percentile) {
                return value.value(TimeUnit.MILLISECONDS);
            }
        }
        return null;
    }
}

