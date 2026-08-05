package com.kinplatform.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * Registrador de métricas del Knowledge Engine (Fase 7 — observabilidad).
 * Toda la instrumentación vive en infraestructura; el dominio es POJO puro.
 *
 * <p>Usa Micrometer ({@link MeterRegistry}); si el registro es {@code null}
 * todas las operaciones son no-op (defensivo). Los nombres siguen el prefijo
 * {@code kin.knowledge.*}. Las métricas de proveedores se registran por
 * {@code ProviderType} (abstracto), nunca por proveedor concreto.</p>
 */
public final class KnowledgeMetrics {

    private static final MeterRegistry FALLBACK =
        new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    private final MeterRegistry registry;

    public KnowledgeMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // ---- Ciclo total ----
    public void cycle(long durationMs, String result) {
        timer("kin.knowledge.cycle", "result", result).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ---- Latencias por etapa ----
    public void stage(String stage, long durationMs) {
        timer("kin.knowledge.stage", "stage", stage).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ---- Planner ----
    public void plannerIntent(String intent) {
        counter("kin.knowledge.planner.intent", "intent", intent).increment();
    }

    public void plannerStrategy(String strategy) {
        counter("kin.knowledge.planner.strategy", "strategy", strategy).increment();
    }

    public void plannerQueryStrategy(String strategy) {
        counter("kin.knowledge.planner.query_strategy", "strategy", strategy).increment();
    }

    public void plannerFacets(int count) {
        counter("kin.knowledge.planner.facets").increment(count);
    }

    public void plannerQueries(int count) {
        counter("kin.knowledge.planner.queries").increment(count);
    }

    // ---- Policy Engine ----
    public void policyDecision(String decision) {
        counter("kin.knowledge.policy.decision", "decision", decision).increment();
    }

    // ---- Cache ----
    public void cacheHit() {
        counter("kin.knowledge.cache_hit").increment();
    }

    public void cacheMiss() {
        counter("kin.knowledge.cache_miss").increment();
    }

    public void cacheAvoidedQuery() {
        counter("kin.knowledge.cache_avoided_queries").increment();
    }

    public void cacheSaved() {
        counter("kin.knowledge.cache_saved").increment();
    }

    // ---- Providers (por ProviderType abstracto) ----
    public void providerRequest(String providerType) {
        counter("kin.knowledge.provider.requests", "type", providerType).increment();
    }

    public void providerError(String providerType) {
        counter("kin.knowledge.provider.errors", "type", providerType).increment();
    }

    public void providerTimeout(String providerType) {
        counter("kin.knowledge.provider.timeouts", "type", providerType).increment();
    }

    public void providerLatency(String providerType, long durationMs) {
        timer("kin.knowledge.provider.latency", "type", providerType)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void providerRegistryLatency(String providerType, long durationMs) {
        timer("kin.knowledge.provider.registry", "type", providerType)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ---- Calidad ----
    public void candidatesReceived(int count) {
        counter("kin.knowledge.quality.candidates_received").increment(count);
    }

    public void candidatesDiscarded(int count) {
        counter("kin.knowledge.quality.candidates_discarded").increment(count);
    }

    public void sourcesAccepted(int count) {
        counter("kin.knowledge.quality.sources_accepted").increment(count);
    }

    public void sourcesRejected(int count) {
        counter("kin.knowledge.quality.sources_rejected").increment(count);
    }

    public void averageScore(double value) {
        summary("kin.knowledge.quality.average_score").record(value);
    }

    public void averageConfidence(double value) {
        summary("kin.knowledge.quality.average_confidence").record(value);
    }

    // ---- Orchestrator ----
    public void stateTransition(String state) {
        counter("kin.knowledge.orchestrator.state", "state", state).increment();
    }

    public void degraded() {
        counter("kin.knowledge.orchestrator.degraded").increment();
    }

    public void offlineMode() {
        counter("kin.knowledge.orchestrator.offline_mode").increment();
    }

    public void gracefulDegradation() {
        counter("kin.knowledge.orchestrator.graceful_degradation").increment();
    }

    public void failFast() {
        counter("kin.knowledge.orchestrator.fail_fast").increment();
    }

    public void budgetExhausted() {
        counter("kin.knowledge.orchestrator.budget_exhausted").increment();
    }

    public void providersSelected(int count) {
        counter("kin.knowledge.orchestrator.providers_selected").increment(count);
    }

    // ---- Citation ----
    public void citationStyle(String style) {
        counter("kin.knowledge.citation.style", "style", style).increment();
    }

    public void citationEntries(int count) {
        counter("kin.knowledge.citation.entries").increment(count);
    }

    public void citationBundles() {
        counter("kin.knowledge.citation.bundles").increment();
    }

    private MeterRegistry registry() {
        return registry == null ? FALLBACK : registry;
    }

    /**
     * Lee el valor actual de un contador (lectura de diagnóstico/tests). Devuelve
     * {@code 0.0} si el contador no existe.
     */
    public double count(String name, String... tags) {
        Counter counter = registry().find(name).tags(tags).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(registry());
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name).tags(tags).register(registry());
    }

    private DistributionSummary summary(String name, String... tags) {
        return DistributionSummary.builder(name).tags(tags).register(registry());
    }
}
