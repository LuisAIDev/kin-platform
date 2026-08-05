package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.engine.DomainContextAssembler;
import com.kinplatform.kin.knowledge.engine.DomainContextRanker;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceRegistryAdapter;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.knowledge.engine.SourceValidatorAdapter;
import com.kinplatform.kin.knowledge.policy.KnowledgePolicyEngine;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;
import com.kinplatform.kin.knowledge.planner.QueryPlanner;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeOrchestratorIntegrationTest {

    private static final OffsetDateTime PUBLISHED = OffsetDateTime.now().minusDays(5);

    private SourceValidator validator() {
        return new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json"));
    }

    private KnowledgeCandidate candidate(String sourceId, String sourceType) {
        return new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
            sourceId, "Fuente", "https://example.com/report", PUBLISHED,
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, sourceType));
    }

    private KnowledgeCandidate candidateWith(String sourceId, String sourceType, String url) {
        return new KnowledgeCandidate("Dato verificado de mercado colombiano. ".repeat(6),
            sourceId, "Fuente", url, PUBLISHED,
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, sourceType));
    }

    private KnowledgeOrchestrator wired(List<? extends KnowledgeSource> sources, KnowledgeRepository repository) {
        return new KnowledgeOrchestrator(
            new QueryPlanner(), new KnowledgePolicyEngine(),
            new SourceRegistryAdapter(new SourceRegistry(new ArrayList<>(sources))), repository,
            new SourceValidatorAdapter(validator()),
            new DomainContextRanker(), new DomainContextAssembler());
    }

    private KnowledgeRequest request(String topic) {
        return KnowledgeRequest.of(topic, List.of());
    }

    private OrchestrationRequest orchestration(KnowledgeRequest request, OrchestrationStrategy strategy,
                                               ExecutionEnvironment environment) {
        return OrchestrationRequest.of(request, PolicyConfig.defaults(), strategy, environment);
    }

    @Test
    void orchestrator_deberiaPlanificarConElQueryPlanner() {
        var orchestrator = wired(List.of(), new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertFalse(result.orchestration().plan().isEmpty());
        assertTrue(result.orchestration().plan().queries().stream()
            .allMatch(query -> query.providerType() != null));
    }

    @Test
    void orchestrator_deberiaConsultarElRegistroYAdquirir() {
        var repository = new InMemoryRepository();
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), repository);

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertFalse(result.knowledge().isEmpty());
        assertEquals(1, result.knowledge().factCount());
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, result.knowledge().facts().get(0).trust());
    }

    @Test
    void orchestrator_deberiaValidarAntesDelRanking() {
        var sources = List.of(new StubSource(List.of(
            candidateWith("src-invalid", "official", "http://inseguro.com/x"),
            candidate("src-oficial", "official"))));
        var orchestrator = wired(sources, new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertFalse(result.knowledge().facts().stream()
            .anyMatch(fact -> fact.sourceId().equals("src-invalid")));
        assertTrue(result.knowledge().validations().stream().anyMatch(validation -> !validation.accepted()));
    }

    @Test
    void orchestrator_deberiaRankearPorConfianza() {
        var sources = List.of(new StubSource(List.of(
            candidate("src-sec", "secondary"),
            candidate("src-oficial", "official"))));
        var orchestrator = wired(sources, new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertEquals("src-oficial", result.knowledge().facts().get(0).sourceId());
    }

    @Test
    void orchestrator_deberiaEnsamblarYEstamparGenerador() {
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertTrue(result.knowledge().explanation().contains("1 de 1"));
        assertEquals("KnowledgeEngine", result.knowledge().generatedBy());
    }

    @Test
    void cacheMiss_deberiaAdquirirYGuardar() {
        var repository = new InMemoryRepository();
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), repository);

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertFalse(result.cacheHit());
        assertFalse(result.knowledge().isEmpty());
        assertFalse(repository.isEmpty());
    }

    @Test
    void cacheHit_deberiaReutilizarSinConsultarFuentes() {
        var repository = new InMemoryRepository();
        var sources = List.of(new StubSource(List.of(candidate("src-1", "official"))));
        var orchestrator = wired(sources, repository);

        orchestrator.coordinateWithResult(orchestration(request("Mercado del café colombiano"),
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online()));
        StubSource source = (StubSource) sources.get(0);
        int callsAfterFirst = source.calls();

        var hit = orchestrator.coordinateWithResult(orchestration(request("Mercado del café colombiano"),
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online()));

        assertTrue(hit.cacheHit());
        assertFalse(hit.knowledge().isEmpty());
        assertEquals(callsAfterFirst, source.calls());
    }

    @Test
    void queryPolicyNoConsultar_deberiaFinalizarSinAdquirir() {
        var repository = new InMemoryRepository();
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), repository);

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Explícame Scrum"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));

        assertTrue(result.knowledge().isEmpty());
        assertFalse(result.cacheHit());
        assertTrue(repository.isEmpty());
    }

    @Test
    void offline_conExternas_deberiaDegradar() {
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.offline()));

        assertTrue(result.knowledge().isEmpty());
        assertTrue(result.orchestration().degraded());
    }

    @Test
    void pdfLocal_offline_deberiaFuncionarLocal() {
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Analiza este PDF"), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.offline()));

        assertFalse(result.knowledge().isEmpty());
        assertFalse(result.orchestration().degraded());
    }

    @Test
    void failFast_conProveedorCaido_deberiaFallar() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(com.kinplatform.kin.knowledge.planner.ProviderType.STATISTICS, ProviderHealth.DOWN));
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.FAIL_FAST, env));

        assertTrue(result.orchestration().failed());
    }

    @Test
    void graceful_conProveedorCaido_deberiaDegradarSinFallar() {
        var env = new ExecutionEnvironment(true, true,
            Map.of(com.kinplatform.kin.knowledge.planner.ProviderType.STATISTICS, ProviderHealth.DOWN));
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), new InMemoryRepository());

        var result = orchestrator.coordinateWithResult(
            orchestration(request("Mercado del café colombiano"), OrchestrationStrategy.GRACEFUL_DEGRADATION, env));

        assertTrue(result.orchestration().completed());
        assertTrue(result.orchestration().degraded());
    }

    @Test
    void determinismo_mismaEntradaMismoResultado() {
        var orchestrator = wired(List.of(new StubSource(List.of(candidate("src-1", "official")))), new NoCacheRepository());
        var request = orchestration(request("Mercado del café colombiano"),
            OrchestrationStrategy.GRACEFUL_DEGRADATION, ExecutionEnvironment.online());

        var r1 = orchestrator.coordinateWithResult(request);
        var r2 = orchestrator.coordinateWithResult(request);

        assertEquals(r1.knowledge(), r2.knowledge());
        assertEquals(r1.orchestration(), r2.orchestration());
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

    private static final class InMemoryRepository implements KnowledgeRepository {
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

    private static final class NoCacheRepository implements KnowledgeRepository {
        @Override
        public Optional<KnowledgeResult> find(KnowledgeQuery query) {
            return Optional.empty();
        }

        @Override
        public void save(KnowledgeResult result, Duration ttl) {
        }
    }
}
