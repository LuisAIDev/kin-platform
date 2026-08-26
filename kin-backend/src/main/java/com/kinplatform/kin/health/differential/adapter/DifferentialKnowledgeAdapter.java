package com.kinplatform.kin.health.differential.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de infraestructura de conocimiento del diagnóstico diferencial
 * (ADR-029): implementa el puerto {@link KnowledgeSource} del KnowledgeEngine.
 *
 * <p>Offline-first y SSRF-safe: por defecto lee un dataset empaquetado
 * ({@code data/differential-catalog.json}) y lo expone como candidatos cuyo
 * {@code content} es JSON estructurado de factores de riesgo y pruebas. Si el
 * operador habilita una fuente externa, consulta esa API médica y, ante
 * cualquier error, degrada con gracia al bundle. No es un bean del
 * {@code SourceRegistry} principal: se instancia en {@code DifferentialConfig}.</p>
 */
public class DifferentialKnowledgeAdapter implements KnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(DifferentialKnowledgeAdapter.class);

    private final String sourceId;
    private final String sourceName;
    private final boolean externalEnabled;
    private final String baseUrl;
    private final String bundledResource;
    private final ObjectMapper objectMapper;

    public DifferentialKnowledgeAdapter(
            String sourceId, String sourceName, boolean externalEnabled, String baseUrl, String bundledResource) {
        this(sourceId, sourceName, externalEnabled, baseUrl, bundledResource, new ObjectMapper());
    }

    public DifferentialKnowledgeAdapter(
            String sourceId,
            String sourceName,
            boolean externalEnabled,
            String baseUrl,
            String bundledResource,
            ObjectMapper objectMapper) {
        this.sourceId = sourceId == null ? "health-differential" : sourceId;
        this.sourceName = sourceName == null ? "Health Differential Catalog" : sourceName;
        this.externalEnabled = externalEnabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.bundledResource = bundledResource == null || bundledResource.isBlank()
                ? "data/differential-catalog.json"
                : bundledResource;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null || query.topic() == null || query.topic().isBlank()) {
            return List.of();
        }
        String content = externalEnabled && !baseUrl.isBlank() ? fetchExternal(query.topic()) : fetchBundled();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(new KnowledgeCandidate(
                content,
                sourceId,
                sourceName,
                "https://www.who.int/health-topics/differential",
                OffsetDateTime.now(),
                "application/json",
                Map.of(
                        SourceValidator.META_HTTP_STATUS, "200",
                        SourceValidator.META_SOURCE_TYPE, "official_public",
                        SourceValidator.META_CATEGORY, "SALUD")));
    }

    private String fetchExternal(String topic) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            String url = baseUrl.endsWith("/") ? baseUrl + topic : baseUrl + "/" + topic;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DifferentialKnowledgeAdapter: HTTP {} desde {}", response.statusCode(), url);
                return "";
            }
            return response.body();
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("DifferentialKnowledgeAdapter: fallo externo, degrada al bundle ({})", ex.getMessage());
            return "";
        }
    }

    private String fetchBundled() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(bundledResource)) {
            if (in == null) {
                log.warn("DifferentialKnowledgeAdapter: recurso {} no encontrado", bundledResource);
                return "";
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("DifferentialKnowledgeAdapter: no se pudo leer el bundle ({})", ex.getMessage());
            return "";
        }
    }
}
