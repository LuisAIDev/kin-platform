package com.kinplatform.ai.knowledge.adapter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

/**
 * Métricas de adaptadores de conocimiento (ADR-021, infraestructura).
 *
 * <p>Expone el contrato de observabilidad solicitado por la capacidad
 * "Adaptadores de conocimiento externo" con nombres {@code kin.knowledge.adapter.*}:
 * requests, success, failure, timeout, rejected, cache.hit, cache.miss y
 * latency. Usa Micrometer ({@link MeterRegistry}) sin duplicar infraestructura
 * (mismo mecanismo que {@code KnowledgeMetrics}); si el registro es {@code null}
 * todas las operaciones son no-op (defensivo).</p>
 */
public final class KnowledgeAdapterMetrics {

    private static final MeterRegistry FALLBACK = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    private final MeterRegistry registry;

    public KnowledgeAdapterMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Consulta HTTP saliente iniciada hacia una fuente. */
    public void request(String source) {
        counter("kin.knowledge.adapter.requests", source).increment();
    }

    /** Consulta HTTP completada con estado 2xx. */
    public void success(String source) {
        counter("kin.knowledge.adapter.success", source).increment();
    }

    /** Consulta HTTP completada con estado 4xx/5xx (fallo controlado). */
    public void failure(String source) {
        counter("kin.knowledge.adapter.failure", source).increment();
    }

    /** Consulta HTTP abortada por timeout o por tamaño de respuesta excedido. */
    public void timeout(String source) {
        counter("kin.knowledge.adapter.timeout", source).increment();
    }

    /** Destino rechazado por la guardia SSRF/allowlist (antes de conectar). */
    public void rejected(String source) {
        counter("kin.knowledge.adapter.rejected", source).increment();
    }

    /** Acierto de caché en el {@code KnowledgeRepository}. */
    public void cacheHit() {
        counter("kin.knowledge.adapter.cache.hit").increment();
    }

    /** Fallo de caché en el {@code KnowledgeRepository}. */
    public void cacheMiss() {
        counter("kin.knowledge.adapter.cache.miss").increment();
    }

    /** Latencia de una consulta HTTP exitosa hacia una fuente. */
    public void latency(String source, long durationMs) {
        timer("kin.knowledge.adapter.latency", source).record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** Lectura de diagnóstico/tests del valor actual de un contador. */
    public double count(String name, String source) {
        Counter counter = registry().find(name).tag("source", source).counter();
        return counter == null ? 0.0 : counter.count();
    }

    public double count(String name) {
        Counter counter = registry().find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private MeterRegistry registry() {
        return registry == null ? FALLBACK : registry;
    }

    private Counter counter(String name, String source) {
        return Counter.builder(name).tag("source", source).register(registry());
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(registry());
    }

    private Timer timer(String name, String source) {
        return Timer.builder(name).tag("source", source).register(registry());
    }
}
