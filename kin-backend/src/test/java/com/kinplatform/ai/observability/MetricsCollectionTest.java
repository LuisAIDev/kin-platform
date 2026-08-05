package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.citation.CitationEngine;
import com.kinplatform.kin.knowledge.citation.CitationStyle;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCollectionTest {

    private final SourceValidator validator = new SourceValidator(Set.of("example.com"),
        Duration.ofDays(365), Set.of("application/json"));

    private KnowledgeCandidate candidate(String sourceId) {
        return new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
            sourceId, "Fuente", "https://example.com/" + sourceId, OffsetDateTime.now().minusDays(5),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    private KnowledgeSource source(KnowledgeCandidate... candidates) {
        return new StubSource(List.of(candidates));
    }

    @Test
    void deberiaRegistrarConteoDeCandidatosYFuentes() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(new SourceRegistry(List.of(
            source(candidate("a"), candidate("b")))), validator, null, meters);

        observable.acquire(com.kinplatform.kin.knowledge.KnowledgeRequest.of("Mercado del café", List.of()));

        assertEquals(2.0, meters.get("kin.knowledge.quality.candidates_received").counter().count(), 1e-9);
        assertEquals(2.0, meters.get("kin.knowledge.quality.sources_accepted").counter().count(), 1e-9);
        assertTrue(meters.get("kin.knowledge.quality.average_confidence").summary().totalAmount() > 0);
    }

    @Test
    void deberiaRegistrarCandidatosDescartados() {
        var meters = new SimpleMeterRegistry();
        var invalid = new KnowledgeCandidate("Dato.", "bad", "Fuente", "http://inseguro.com/x",
            OffsetDateTime.now(), "application/json", Map.of());
        var observable = ObservableKnowledgeRuntime.create(new SourceRegistry(List.of(
            source(invalid))), validator, null, meters);

        observable.acquire(com.kinplatform.kin.knowledge.KnowledgeRequest.of("Mercado del café", List.of()));

        assertEquals(1.0, meters.get("kin.knowledge.quality.candidates_discarded").counter().count(), 1e-9);
        assertEquals(1.0, meters.get("kin.knowledge.quality.sources_rejected").counter().count(), 1e-9);
        assertEquals(0.0, meters.get("kin.knowledge.quality.sources_accepted").counter().count(), 1e-9);
    }

    @Test
    void deberiaRegistrarDecisionesDePoliticaYProveedores() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(new SourceRegistry(List.of(
            source(candidate("a")))), validator, null, meters);

        observable.acquire(com.kinplatform.kin.knowledge.KnowledgeRequest.of("Mercado del café", List.of()));

        assertTrue(meters.get("kin.knowledge.policy.decision").tag("decision", "EXTERNAL").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.provider.requests").tag("type", "STATISTICS").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.provider.latency").tag("type", "STATISTICS").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.orchestrator.providers_selected").counter().count() >= 1);
    }

    @Test
    void deberiaRegistrarMetricasDelCitationEngine() {
        var meters = new SimpleMeterRegistry();
        var metrics = new KnowledgeMetrics(meters);
        var engine = new TimedCitationEngine(metrics);
        var result = new com.kinplatform.kin.knowledge.KnowledgeResult(List.of(
            com.kinplatform.kin.knowledge.KnowledgeFact.of("Dato.", "src-1", "https://example.com/x",
                OffsetDateTime.now(), com.kinplatform.kin.knowledge.SourceTrust.OFFICIAL_PUBLIC, "MERCADO")),
            List.of("src-1"), List.of(), 1.0, "ok", "KnowledgeEngine", "v1");

        var citation = engine.produce(result, null, CitationStyle.FOOTNOTE);

        assertTrue(!citation.isEmpty());
        assertTrue(meters.get("kin.knowledge.citation.bundles").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.citation.style").tag("style", "FOOTNOTE").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.citation.entries").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "citation").timer().count() >= 1);
    }

    @Test
    void metricsConRegistryNulo_deberianSerNoOp() {
        var metrics = new KnowledgeMetrics(null);

        metrics.cacheHit();
        metrics.providerRequest("GOVERNMENT");
        metrics.stage("planner", 5);

        assertTrue(true);
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
