package com.kinplatform.common.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.platform.enrichment.EnrichmentEngine;
import com.kinplatform.platform.enrichment.EnrichmentInput;
import com.kinplatform.platform.enrichment.EnrichmentResult;
import com.kinplatform.platform.enrichment.EvidenceCategory;
import com.kinplatform.platform.enrichment.FactRanker;
import com.kinplatform.common.knowledge.KnowledgeInput;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import com.kinplatform.common.knowledge.engine.KnowledgeGateway;
import com.kinplatform.common.knowledge.engine.SourceRegistry;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba de trazabilidad end-to-end (ADR-021): una fuente HTTP controlada
 * (servidor local determinista) → {@link SecureHttpClient} + guard SSRF →
 * {@link HttpKnowledgeSourceAdapter} → {@link SourceRegistry} → {@link SourceValidator}
 * (allowlist) → {@link KnowledgeGateway} → {@link KnowledgeEngine} →
 * {@link EnrichmentEngine}. Verifica que el pipeline obtiene hechos verificados
 * y evidencia enriquecida sin que el LLM participe en ninguna decisión.
 */
class KnowledgeHttpEndToEndTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void serveItems(String json) {
        server.createContext("/search", exchange -> {
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    private KnowledgeEngine engine(SourceConnectionGuard guard) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        SecureHttpClient secureClient = new SecureHttpClient(
                jdkClient,
                guard,
                "src-http",
                Duration.ofSeconds(2),
                16_384,
                3,
                Set.of("application/json"),
                0,
                Duration.ofMillis(50),
                new KnowledgeAdapterMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        HttpKnowledgeSourceAdapter adapter = new HttpKnowledgeSourceAdapter(
                "src-http",
                "Fuente Autorizada",
                "http://localhost:" + port + "/search",
                secureClient,
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(new ObjectMapper()));
        SourceRegistry registry = new SourceRegistry(List.of(adapter));
        SourceValidator validator = new SourceValidator(Set.of("data.autorizado.com"), null, Set.of());
        return new KnowledgeEngine(new KnowledgeGateway(registry, validator));
    }

    private static String itemsJson() {
        String published = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5))
                .toString();
        return "{\"items\":["
                + "{\"content\":\"El mercado retail colombiano crece 8% anual.\","
                + "\"url\":\"https://data.autorizado.com/retail\",\"publishedAt\":\"" + published + "\"},"
                + "{\"content\":\"La demanda de consumo masivo se concentra en Bogotá.\","
                + "\"url\":\"https://data.autorizado.com/demanda\",\"publishedAt\":\"" + published + "\"}]}";
    }

    private static KnowledgeRequest request() {
        return new KnowledgeRequest("retail", Set.of(), List.of("mercado", "retail"), 5, Duration.ofDays(365));
    }

    @Test
    void cadenaCompleta_deberiaProducirHechosVerificadosYEvidencia() {
        serveItems(itemsJson());
        KnowledgeEngine engine = engine(new SourceConnectionGuard(Set.of(), true));

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertFalse(knowledge.isEmpty());
        assertEquals(2, knowledge.factCount());
        assertEquals("src-http", knowledge.sourcesUsed().get(0));
        assertEquals(
                com.kinplatform.common.knowledge.SourceTrust.UNVERIFIED,
                knowledge.facts().get(0).trust());

        EnrichmentEngine enrichmentEngine = new EnrichmentEngine(new FactRanker());
        EnrichmentResult enriched = enrichmentEngine.evaluate(
                EnrichmentInput.of(ProjectContext.fromProject("Retail Bogotá", "Retail", "Consumo masivo"), knowledge));

        assertFalse(enriched.isEmpty());
        assertNotNull(enriched.rankFor(EvidenceCategory.MARKET));
        assertTrue(enriched.sourcesUsed().contains("src-http"));
    }

    @Test
    void fuenteCaida_deberiaDegradarSinRomperElAnalisis() {
        server.stop(0);
        server = null;
        KnowledgeEngine engine = engine(new SourceConnectionGuard(Set.of(), true));

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertTrue(knowledge.isEmpty());
        assertTrue(knowledge.facts().isEmpty());
        assertNotNull(knowledge.explanation());

        EnrichmentEngine enrichmentEngine = new EnrichmentEngine(new FactRanker());
        EnrichmentResult enriched = enrichmentEngine.evaluate(
                EnrichmentInput.of(ProjectContext.fromProject("Retail Bogotá", "Retail", "Consumo masivo"), knowledge));

        assertTrue(enriched.isEmpty());
    }

    @Test
    void destinoFueraDeAllowlist_deberiaRechazarseEnElGuard() {
        serveItems(itemsJson());
        // Guard sin loopback y allowlist vacía: la conexión localhost se rechaza.
        KnowledgeEngine engine = engine(new SourceConnectionGuard(Set.of("solo-https-autorizado.com"), false));

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertTrue(knowledge.isEmpty());
    }
}





