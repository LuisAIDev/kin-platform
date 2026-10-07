package com.kinplatform.common.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.knowledge.KnowledgeInput;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import com.kinplatform.common.knowledge.engine.KnowledgeGateway;
import com.kinplatform.common.knowledge.engine.SourceRegistry;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Verificación de RED REAL (ADR-021): ejercita la cadena de adaptadores reales
 * ({@link SecureHttpClient} + {@link SourceConnectionGuard} +
 * {@link HttpKnowledgeSourceAdapter} + {@link SourceValidator} +
 * {@link KnowledgeGateway} + {@link KnowledgeEngine}) contra una fuente externa
 * controlada, pública y estable: {@code https://httpbin.org/json} (permite la
 * allowlist documentada {@code httpbin.org}).
 *
 * <p>Test <em>gated</em>: solo se ejecuta cuando {@code KIN_TEST_REAL_NETWORK=true}
 * (requiere salida a Internet). Sin la variable, el test se omite y la suite
 * por defecto permanece offline/determinista.</p>
 */
@EnabledIfEnvironmentVariable(named = "KIN_TEST_REAL_NETWORK", matches = "true")
class KnowledgeHttpRealNetworkTest {

    static final String SOURCE_ID = "real-http";
    static final String BASE_URL = "https://httpbin.org/json";

    private static KnowledgeRequest request() {
        return new KnowledgeRequest("retail", Set.of(), List.of("mercado", "retail"), 5, Duration.ofDays(365));
    }

    /**
     * Decoder específico para el esquema real de httpbin.org/json
     * ({@code {"slideshow":{"slides":[{"title":...}]}}}), documentado como el
     * decoder que un operador adaptaría por fuente autorizada. La URL de cada
     * hecho se deriva de la fuente (https, host en allowlist).
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>>
            slideshowDecoder(ObjectMapper mapper) {
        return response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            try {
                JsonNode root = mapper.readTree(response.body());
                JsonNode slides = root.path("slideshow").path("slides");
                if (!slides.isArray()) {
                    return List.of();
                }
                var out = new ArrayList<HttpKnowledgeSourceAdapter.HttpItem>();
                for (JsonNode slide : slides) {
                    String title = slide.path("title").asText("");
                    if (title.isBlank()) {
                        continue;
                    }
                    out.add(new HttpKnowledgeSourceAdapter.HttpItem(
                            title, "https://httpbin.org/json#" + title.hashCode(), OffsetDateTime.now()));
                }
                return List.copyOf(out);
            } catch (Exception ex) {
                return List.of();
            }
        };
    }

    private static KnowledgeEngine engine(
            Set<String> allowedDomains,
            String baseUrl,
            Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>> decoder,
            boolean allowLoopback) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        SecureHttpClient secureClient = new SecureHttpClient(
                jdkClient,
                new SourceConnectionGuard(allowedDomains, allowLoopback),
                SOURCE_ID,
                Duration.ofSeconds(5),
                16_384,
                3,
                Set.of("application/json"),
                0,
                Duration.ofMillis(50),
                new KnowledgeAdapterMetrics(new SimpleMeterRegistry()));
        HttpKnowledgeSourceAdapter adapter =
                new HttpKnowledgeSourceAdapter(SOURCE_ID, "Real HTTP", baseUrl, secureClient, decoder);
        SourceRegistry registry = new SourceRegistry(List.of(adapter));
        SourceValidator validator = new SourceValidator(allowedDomains, null, Set.of("application/json"));
        return new KnowledgeEngine(new KnowledgeGateway(registry, validator));
    }

    @Test
    void fuentePublicaReal_deberiaProducirHechosVerificados() {
        KnowledgeEngine engine = engine(Set.of("httpbin.org"), BASE_URL, slideshowDecoder(new ObjectMapper()), false);

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertFalse(knowledge.isEmpty(), "la fuente real debería aportar hechos (offline-first no debe truncar)");
        assertTrue(knowledge.factCount() >= 1);
        assertTrue(knowledge.sourcesUsed().contains(SOURCE_ID));
        assertNotNull(knowledge.explanation());
    }

    @Test
    void allowlistVacia_deberiaRechazarLaConexionAntesDeRed() {
        KnowledgeEngine engine = engine(Set.of(), BASE_URL, slideshowDecoder(new ObjectMapper()), false);

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertTrue(knowledge.isEmpty(), "allowlist vacía = offline-first: sin candidatos externos");
    }

    @Test
    void dominioFueraDeAllowlist_deberiaRechazarseEnElGuard() {
        KnowledgeEngine engine =
                engine(Set.of("solo-autorizado.com"), BASE_URL, slideshowDecoder(new ObjectMapper()), false);

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertTrue(knowledge.isEmpty(), "dominio fuera de allowlist se rechaza antes de conectar");
    }

    @Test
    void fuenteConError5xxReal_deberiaDegradarOfflineFirst() {
        KnowledgeEngine engine = engine(
                Set.of("httpbin.org"), "https://httpbin.org/status/500", slideshowDecoder(new ObjectMapper()), false);

        KnowledgeResult knowledge = engine.evaluate(KnowledgeInput.of(request()));

        assertTrue(knowledge.isEmpty(), "error real 5xx degrada a KnowledgeResult.empty()");
        assertNotNull(knowledge.explanation(), "el fallo controlado conserva un motivo trazable");
    }
}


