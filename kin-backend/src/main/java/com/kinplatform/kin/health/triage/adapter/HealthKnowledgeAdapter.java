package com.kinplatform.kin.health.triage.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.KnowledgeQuery;
import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de infraestructura de conocimiento médico (ADR-028, fase
 * profesional): implementa el puerto {@link KnowledgeSource} del KnowledgeEngine.
 *
 * <p>Offline-first y SSRF-safe: por defecto lee un dataset estructurado
 * empaquetado ({@code data/triage-catalog-extended.json}, CIE-10/guías abiertas)
 * y lo expone como candidatos cuyo {@code content} es JSON estructurado de
 * condiciones. Si el operador habilita una fuente externa
 * ({@code kin.health.triage.catalog.external-enabled=true} + {@code base-url}),
 * consulta esa API médica con {@link HttpClient} y, ante cualquier error o
 * respuesta no parseable, degrada con gracia al dataset empaquetado (nunca
 * rompe el catálogo local).</p>
 *
 * <p>No es un bean de {@code KnowledgeSource} del registro principal: se
 * instancia en {@code TriageConfig} y lo consume exclusivamente el
 * {@code TriageCatalogUpdateService}, evitando alterar el pipeline de
 * conocimiento existente.</p>
 */
public class HealthKnowledgeAdapter implements KnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(HealthKnowledgeAdapter.class);

    private final String sourceId;
    private final String sourceName;
    private final boolean externalEnabled;
    private final String baseUrl;
    private final String bundledResource;
    private final ObjectMapper objectMapper;

    public HealthKnowledgeAdapter(
            String sourceId, String sourceName, boolean externalEnabled, String baseUrl, String bundledResource) {
        this(sourceId, sourceName, externalEnabled, baseUrl, bundledResource, new ObjectMapper());
    }

    public HealthKnowledgeAdapter(
            String sourceId,
            String sourceName,
            boolean externalEnabled,
            String baseUrl,
            String bundledResource,
            ObjectMapper objectMapper) {
        this.sourceId = sourceId == null ? "health-catalog" : sourceId;
        this.sourceName = sourceName == null ? "Health Catalog" : sourceName;
        this.externalEnabled = externalEnabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.bundledResource = bundledResource == null || bundledResource.isBlank()
                ? "data/triage-catalog-extended.json"
                : bundledResource;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null || query.topic() == null || query.topic().isBlank()) {
            return List.of();
        }
        List<HealthCatalogEntry> entries =
                externalEnabled && !baseUrl.isBlank() ? fetchExternal(query.topic()) : fetchBundled();
        return toCandidates(entries);
    }

    /**
     * Consulta la API médica externa configurada. Ante cualquier fallo devuelve
     * vacío (el llamador degrada al bundle).
     */
    private List<HealthCatalogEntry> fetchExternal(String topic) {
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
                log.warn("HealthKnowledgeAdapter: HTTP {} desde {}", response.statusCode(), url);
                return List.of();
            }
            return parseEntries(response.body());
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("HealthKnowledgeAdapter: fallo de fuente externa, degrada al bundle ({})", ex.getMessage());
            return List.of();
        }
    }

    private List<HealthCatalogEntry> fetchBundled() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(bundledResource)) {
            if (in == null) {
                log.warn("HealthKnowledgeAdapter: recurso {} no encontrado", bundledResource);
                return List.of();
            }
            return parseEntries(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.warn("HealthKnowledgeAdapter: no se pudo leer el bundle ({})", ex.getMessage());
            return List.of();
        }
    }

    private List<HealthCatalogEntry> parseEntries(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            var root = objectMapper.readTree(body);
            var out = new ArrayList<HealthCatalogEntry>();
            var conditions = root.path("conditions");
            if (conditions.isArray()) {
                for (var node : conditions) {
                    HealthCatalogEntry entry = objectMapper.treeToValue(node, HealthCatalogEntry.class);
                    if (entry != null
                            && entry.getName() != null
                            && !entry.getName().isBlank()) {
                        out.add(entry);
                    }
                }
            }
            return List.copyOf(out);
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn("HealthKnowledgeAdapter: JSON de fuente no parseable ({})", ex.getMessage());
            return List.of();
        }
    }

    private List<KnowledgeCandidate> toCandidates(List<HealthCatalogEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<KnowledgeCandidate>();
        for (HealthCatalogEntry entry : entries) {
            try {
                String content = objectMapper.writeValueAsString(entry);
                String url = "https://www.who.int/health-topics/" + slug(entry.getName());
                out.add(new KnowledgeCandidate(
                        content,
                        sourceId,
                        sourceName,
                        url,
                        OffsetDateTime.now(),
                        "application/json",
                        Map.of(
                                SourceValidator.META_HTTP_STATUS, "200",
                                SourceValidator.META_SOURCE_TYPE, "official_public",
                                SourceValidator.META_CATEGORY, "SALUD")));
            } catch (JsonProcessingException ex) {
                log.debug("HealthKnowledgeAdapter: entrada no serializable, se omite ({})", ex.getMessage());
            }
        }
        return List.copyOf(out);
    }

    private static String slug(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}

