package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
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

class KnowledgeGatewayIntegrationTest {

    private final SourceValidator validator = new SourceValidator(Set.of("example.com"),
        Duration.ofDays(365), Set.of("application/json"));

    private KnowledgeCandidate candidate() {
        return new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
            "src-1", "Fuente", "https://example.com/report", OffsetDateTime.now().minusDays(5),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("mercado retail", List.of("retail"));
    }

    @Test
    void gateway_deberiaDelegarElCicloAlOrchestrator() {
        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(source())), validator);

        var result = gateway.acquire(request());

        assertFalse(result.isEmpty());
        assertEquals(1, result.factCount());
        assertTrue(result.explanation().contains("1 de 1"));
    }

    @Test
    void gateway_politicaDeNoConsultar_deberiaDevolverVacio() {
        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(source())), validator);

        var result = gateway.acquire(KnowledgeRequest.of("Explícame Scrum", List.of()));

        assertTrue(result.isEmpty());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void gateway_cacheMiss_deberiaAdquirirYGuardar() {
        var repository = new SingleRepository();
        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(source())), validator, repository);

        var first = gateway.acquire(request());

        assertFalse(first.isEmpty());
        assertFalse(repository.isEmpty());
    }

    @Test
    void gateway_cacheHit_deberiaReutilizarSinConsultarFuentes() {
        var repository = new SingleRepository();
        var source = source();
        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(source)), validator, repository);

        gateway.acquire(request());
        int callsAfterFirst = source.calls();

        var hit = gateway.acquire(request());

        assertFalse(hit.isEmpty());
        assertEquals(callsAfterFirst, source.calls());
        assertFalse(repository.isEmpty());
    }

    @Test
    void gateway_determinismo_deberiaMantenerse() {
        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(source())), validator);

        var r1 = gateway.acquire(request());
        var r2 = gateway.acquire(request());

        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.facts(), r2.facts());
    }

    private StubSource source() {
        return new StubSource(List.of(candidate()));
    }

    private static final class StubSource implements KnowledgeSource {
        private final List<KnowledgeCandidate> candidates;
        private int calls;

        private StubSource(List<KnowledgeCandidate> candidates) {
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
