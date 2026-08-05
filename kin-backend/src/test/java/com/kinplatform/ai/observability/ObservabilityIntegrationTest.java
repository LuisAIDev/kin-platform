package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityIntegrationTest {

    private final SourceValidator validator = new SourceValidator(Set.of("example.com"),
        Duration.ofDays(365), Set.of("application/json"));

    private KnowledgeCandidate candidate() {
        return new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
            "src-1", "Fuente", "https://example.com/reporte", OffsetDateTime.now().minusDays(5),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("Mercado del café colombiano", List.of());
    }

    private KnowledgeSource source() {
        return new StubSource(List.of(candidate()));
    }

    @Test
    void runtimeObservable_deberiaProducirElMismoResultadoQueElGateway() {
        var registry = new SourceRegistry(List.of(source()));
        var plain = new KnowledgeGateway(registry, validator);
        var observable = ObservableKnowledgeRuntime.create(registry, validator, null,
            new SimpleMeterRegistry());

        KnowledgeResult plainResult = plain.acquire(request());
        KnowledgeResult observedResult = observable.acquire(request());

        assertEquals(plainResult, observedResult);
        assertFalse(observedResult.isEmpty());
    }

    @Test
    void runtimeObservable_deberiaRegistrarMetricasDelCicloYEtapas() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(source())), validator, null, meters);

        observable.acquire(request());

        assertTrue(meters.get("kin.knowledge.cycle").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "planner").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "policy").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "cache").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "validation").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "ranking").timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.stage").tag("stage", "assembler").timer().count() >= 1);
    }

    @Test
    void runtimeObservable_deberiaRegistrarPlannerYCalidad() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(source())), validator, null, meters);

        observable.acquire(request());

        assertTrue(meters.get("kin.knowledge.planner.intent").tag("intent", "MERCADO").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.planner.strategy").tag("strategy", "HYBRID").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.quality.sources_accepted").counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.orchestrator.state").tag("state", "COMPLETED").counter().count() >= 1);
    }

    @Test
    void runtimeObservable_conTemaVacio_deberiaDevolverVacio() {
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(source())), validator, null, new SimpleMeterRegistry());

        var result = observable.acquire(KnowledgeRequest.of("", List.of()));

        assertTrue(result.isEmpty());
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
