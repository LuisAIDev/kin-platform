package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
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

class CacheMetricsTest {

    private final SourceValidator validator = new SourceValidator(Set.of("example.com"),
        Duration.ofDays(365), Set.of("application/json"));

    private KnowledgeCandidate candidate() {
        return new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
            "src-1", "Fuente", "https://example.com/reporte", OffsetDateTime.now().minusDays(5),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    private CountingSource source() {
        return new CountingSource(List.of(candidate()));
    }

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("Mercado del café colombiano", List.of());
    }

    @Test
    void primerAcceso_miss_luegoAcceso_hit() {
        var meters = new SimpleMeterRegistry();
        var repository = new SingleRepository();
        var source = source();
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(source)), validator, repository, meters);

        observable.acquire(request());
        int callsAfterFirst = source.calls();

        observable.acquire(request());

        assertFalse(repository.isEmpty());
        assertEquals(callsAfterFirst, source.calls());
        assertEquals(1.0, meters.get("kin.knowledge.cache_miss").counter().count(), 1e-9);
        assertEquals(1.0, meters.get("kin.knowledge.cache_hit").counter().count(), 1e-9);
        assertEquals(1.0, meters.get("kin.knowledge.cache_avoided_queries").counter().count(), 1e-9);
        assertEquals(1.0, meters.get("kin.knowledge.cache_saved").counter().count(), 1e-9);
    }

    @Test
    void sinRepository_deberiaContarMissSinGuardar() {
        var meters = new SimpleMeterRegistry();
        var observable = ObservableKnowledgeRuntime.create(
            new SourceRegistry(List.of(source())), validator, null, meters);

        observable.acquire(request());

        assertEquals(1.0, meters.get("kin.knowledge.cache_miss").counter().count(), 1e-9);
        var hit = meters.find("kin.knowledge.cache_hit").counter();
        assertTrue(hit == null || hit.count() == 0.0);
        var saved = meters.find("kin.knowledge.cache_saved").counter();
        assertTrue(saved == null || saved.count() == 0.0);
    }

    private static final class CountingSource implements KnowledgeSource {
        private final List<KnowledgeCandidate> candidates;
        private int calls;

        private CountingSource(List<KnowledgeCandidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            calls++;
            return candidates;
        }

        private int calls() {
            return calls;
        }
    }

    private static final class SingleRepository implements KnowledgeRepository {
        private KnowledgeResult stored;

        @Override
        public Optional<KnowledgeResult> find(KnowledgeQuery query) {
            return Optional.ofNullable(stored);
        }

        @Override
        public void save(KnowledgeResult result, Duration ttl) {
            stored = result;
        }

        private boolean isEmpty() {
            return stored == null;
        }
    }
}
