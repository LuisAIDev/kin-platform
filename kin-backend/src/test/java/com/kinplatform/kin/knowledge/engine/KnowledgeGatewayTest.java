package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeGatewayTest {

    private static final Set<String> ALLOWED_DOMAINS = Set.of("example.com");

    private SourceValidator validator() {
        return new SourceValidator(ALLOWED_DOMAINS, Duration.ofDays(365),
            Set.of("application/json", "text/plain"));
    }

    private KnowledgeGateway gateway(SourceRegistry registry) {
        return new KnowledgeGateway(registry, validator());
    }

    private KnowledgeCandidate validCandidate(String sourceId) {
        return new KnowledgeCandidate(longContent(), sourceId, "Fuente",
            "https://example.com/report", OffsetDateTime.now().minusDays(30),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    private KnowledgeCandidate invalidCandidate() {
        return new KnowledgeCandidate(longContent(), "src-x", "Fuente",
            "http://example.com/report", OffsetDateTime.now().minusDays(30),
            "application/json", Map.of());
    }

    private String longContent() {
        return "Dato verificado de mercado. ".repeat(12);
    }

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("mercado retail", List.of("retail", "colombia"));
    }

    private static class StubSource implements KnowledgeSource {
        private final List<KnowledgeCandidate> candidates;
        private KnowledgeQuery lastQuery;

        private StubSource(List<KnowledgeCandidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            this.lastQuery = query;
            return candidates;
        }
    }

    @Test
    void acquire_deberiaConstruirHechosDesdeCandidatosAceptados() {
        var source = new StubSource(List.of(validCandidate("src-1")));
        var gateway = gateway(new SourceRegistry(List.of(source)));

        var result = gateway.acquire(request());

        assertFalse(result.isEmpty());
        assertEquals(1, result.factCount());
        assertEquals("src-1", result.facts().get(0).sourceId());
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, result.facts().get(0).trust());
        assertEquals(List.of("src-1"), result.sourcesUsed());
        assertEquals(1, result.validations().size());
        assertTrue(result.validations().get(0).accepted());
        assertEquals(1.0, result.confidence(), 1e-9);
    }

    @Test
    void acquire_deberiaConsultarCadaFuenteConLaQueryProyectada() {
        var source = new StubSource(List.of(validCandidate("src-1")));
        var gateway = gateway(new SourceRegistry(List.of(source)));

        gateway.acquire(request());

        var query = source.lastQuery;
        assertEquals("mercado retail", query.topic());
        assertEquals(List.of("retail", "colombia"), query.keywords());
        assertEquals(KnowledgeRequest.DEFAULT_LIMIT, query.limit());
        assertEquals(KnowledgeRequest.DEFAULT_TIME_WINDOW, query.timeWindow());
    }

    @Test
    void acquire_deberiaDescartarCandidatosInvalidos() {
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(invalidCandidate())))));

        var result = gateway.acquire(request());

        assertTrue(result.isEmpty());
        assertEquals(0, result.factCount());
        assertTrue(result.sourcesUsed().isEmpty());
        assertEquals(1, result.validations().size());
        assertFalse(result.validations().get(0).accepted());
        assertTrue(result.validations().get(0).reasons().contains("Protocolo HTTPS obligatorio"));
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void acquire_deberiaConsolidarFuentesUsadasSinDuplicados() {
        var candidateA = validCandidate("src-1");
        var candidateB = new KnowledgeCandidate(longContent(), "src-1", "Fuente",
            "https://example.com/second-report", OffsetDateTime.now().minusDays(30),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
        var gateway = gateway(new SourceRegistry(List.of(
            new StubSource(List.of(candidateA)), new StubSource(List.of(candidateB)))));

        var result = gateway.acquire(request());

        assertEquals(2, result.factCount());
        assertEquals(List.of("src-1"), result.sourcesUsed());
    }

    @Test
    void acquire_deberiaNormalizarLaCategoriaDelMeta() {
        var candidate = new KnowledgeCandidate(longContent(), "src-1", "Fuente",
            "https://example.com/report", OffsetDateTime.now(), "application/json",
            Map.of(SourceValidator.META_CATEGORY, "MERCADO"));
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(candidate)))));

        var result = gateway.acquire(request());

        assertEquals("MERCADO", result.facts().get(0).category());
    }

    @Test
    void acquire_deberiaRetornarVacio_cuandoRequestNulo() {
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(validCandidate("src-1"))))));

        var result = gateway.acquire(null);

        assertTrue(result.isEmpty());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void acquire_deberiaRetornarVacio_cuandoTemaVacio() {
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(validCandidate("src-1"))))));

        var result = gateway.acquire(KnowledgeRequest.empty());

        assertTrue(result.isEmpty());
        assertTrue(result.explanation().contains("Tema vacío"));
    }

    @Test
    void acquire_deberiaRetornarVacio_cuandoNoHayFuentesRegistradas() {
        var gateway = gateway(new SourceRegistry(List.of()));

        var result = gateway.acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(result.explanation().contains("No se obtuvieron candidatos"));
    }

    @Test
    void acquire_deberiaTolerarFuenteQueDevuelveNulo() {
        var source = new StubSource(null);
        var gateway = gateway(new SourceRegistry(List.of(source)));

        var result = gateway.acquire(request());

        assertTrue(result.isEmpty());
        assertEquals(0, result.validations().size());
    }

    @Test
    void acquire_deberiaTratarCandidatoNuloComoRechazado() {
        var source = new StubSource(Arrays.asList(validCandidate("src-1"), null));
        var gateway = gateway(new SourceRegistry(List.of(source)));

        var result = gateway.acquire(request());

        assertEquals(1, result.factCount());
        assertEquals(2, result.validations().size());
        assertFalse(result.validations().get(1).accepted());
        assertTrue(result.validations().get(1).reasons().contains("Candidato nulo"));
    }

    @Test
    void acquire_deberiaCalcularConfianzaMayorConFuentesOficiales() {
        var official = new KnowledgeCandidate(longContent(), "src-1", "Fuente",
            "https://example.com/report", OffsetDateTime.now(), "application/json",
            Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
        var unverified = new KnowledgeCandidate(longContent(), "src-2", "Fuente",
            "https://example.com/report", OffsetDateTime.now(), "application/json", Map.of());
        var gatewayOficial = gateway(new SourceRegistry(List.of(new StubSource(List.of(official)))));
        var gatewayNoVerificada = gateway(new SourceRegistry(List.of(new StubSource(List.of(unverified)))));

        double oficial = gatewayOficial.acquire(request()).confidence();
        double noVerificada = gatewayNoVerificada.acquire(request()).confidence();

        assertTrue(oficial > noVerificada);
        assertEquals(1.0, oficial, 1e-9);
        assertEquals(0.82, noVerificada, 1e-9);
    }

    @Test
    void acquire_deberiaContarDescartadasEnLaExplicacion() {
        var source = new StubSource(List.of(validCandidate("src-1"), invalidCandidate()));
        var gateway = gateway(new SourceRegistry(List.of(source)));

        var result = gateway.acquire(request());

        assertTrue(result.explanation().contains("1 de 2 (1 descartados)"));
        assertEquals(0.85, result.confidence(), 1e-9);
    }

    @Test
    void acquire_deberiaSerDeterminista() {
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(validCandidate("src-1"))))));

        var r1 = gateway.acquire(request());
        var r2 = gateway.acquire(request());

        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.explanation(), r2.explanation());
        assertEquals(r1.facts(), r2.facts());
    }

    @Test
    void acquire_deberiaExponerColeccionesInmutables() {
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(validCandidate("src-1"))))));

        var result = gateway.acquire(request());

        assertThrows(UnsupportedOperationException.class, () -> result.facts().add(null));
        assertThrows(UnsupportedOperationException.class, () -> result.sourcesUsed().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> result.validations().add(null));
    }

    @Test
    void constructor_deberiaSoportarRegistryYValidatorNulos() {
        var gateway = new KnowledgeGateway(null, null);

        var result = gateway.acquire(request());

        assertTrue(result.isEmpty());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void acquire_deberiaAplicarPesoSecundarioEnLaConfianza() {
        var secondary = new KnowledgeCandidate(longContent(), "src-2", "Fuente",
            "https://example.com/report", OffsetDateTime.now().minusDays(30),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "secondary"));
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(secondary)))));

        var result = gateway.acquire(request());

        assertEquals(0.91, result.confidence(), 1e-9);
    }

    @Test
    void acquire_deberiaEstamparGeneradorYVersion() {
        var gateway = gateway(new SourceRegistry(List.of(new StubSource(List.of(validCandidate("src-1"))))));

        var result = gateway.acquire(request());

        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(KnowledgeEngine.ENGINE_VERSION, result.engineVersion());
    }
}
