package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeSource;
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

class ProviderMetricsTest {

    private final SourceValidator validator = new SourceValidator(Set.of("example.com"),
        Duration.ofDays(365), Set.of("application/json"));

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("Mercado del café colombiano", List.of());
    }

    @Test
    void proveedorSano_deberiaRegistrarConsultasYLatencia() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(new HealthySource())), validator, null, meters);

        observable.acquire(request());

        assertTrue(meters.get("kin.knowledge.provider.requests").tag("type", "STATISTICS")
            .counter().count() >= 1);
        assertTrue(meters.get("kin.knowledge.provider.latency").tag("type", "STATISTICS")
            .timer().count() >= 1);
        assertTrue(meters.get("kin.knowledge.provider.registry").tag("type", "STATISTICS")
            .timer().count() >= 1);
        var errors = meters.find("kin.knowledge.provider.errors").tag("type", "STATISTICS").counter();
        assertTrue(errors == null || errors.count() == 0.0);
    }

    @Test
    void proveedorFallido_deberiaRegistrarError() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(new FailingSource())), validator, null, meters);

        var result = observable.acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(meters.get("kin.knowledge.provider.errors").tag("type", "STATISTICS")
            .counter().count() >= 1);
    }

    @Test
    void timeout_deberiaRegistrarse() {
        var metrics = new KnowledgeMetrics(new SimpleMeterRegistry());
        var slow = new TimedKnowledgeSource(
            com.kinplatform.kin.knowledge.planner.ProviderType.STATISTICS, new HealthySource(),
            metrics, 0L);

        var result = slow.fetch(com.kinplatform.kin.knowledge.KnowledgeQuery.from(request()));

        assertTrue(!result.isEmpty());
        assertTrue(metrics.count("kin.knowledge.provider.timeouts", "type", "STATISTICS") >= 1);
    }

    private static final class HealthySource implements KnowledgeSource {
        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            return List.of(new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
                "src-1", "Fuente", "https://example.com/reporte", OffsetDateTime.now().minusDays(5),
                "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official")));
        }
    }

    private static final class FailingSource implements KnowledgeSource {
        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            throw new IllegalStateException("Proveedor caído");
        }
    }
}
