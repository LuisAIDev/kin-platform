package com.kinplatform.ai.knowledge.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de los adaptadores reales de conocimiento (ADR-021, infraestructura).
 *
 * <p>Solo se registran cuando el operador los habilita explícitamente
 * (default offline-first):</p>
 * <ul>
 *   <li>{@code kin.knowledge.external-enabled=true} → adaptadores HTTP reales y
 *       seguros ({@link SecureHttpClient} + {@link SourceConnectionGuard});</li>
 *   <li>{@code kin.knowledge.sources[]} → lista de fuentes autorizadas
 *       (multi-fuente, ver {@link KinKnowledgeProperties.SourceConfig}); se
 *       exponen como un único {@link CompositeKnowledgeSource};</li>
 *   <li>{@code kin.knowledge.http.base-url} → fuente única con formato items
 *       (compatibilidad);</li>
 *   <li>{@code kin.knowledge.test-source.enabled=true} → fuente controlada
 *       determinista para dev/test/E2E.</li>
 * </ul>
 *
 * <p>Los beans de {@link KnowledgeSource} se auto-descubren en
 * {@code SourceRegistry} (KinConfig), por lo que el pipeline no cambia: con las
 * fuentes deshabilitadas el registro queda vacío y KIN degrada offline.</p>
 */
@Configuration
@EnableConfigurationProperties(KinKnowledgeProperties.class)
public class KnowledgeHttpAutoConfiguration {

    @Bean
    public KnowledgeAdapterMetrics knowledgeAdapterMetrics(MeterRegistry registry) {
        return new KnowledgeAdapterMetrics(registry);
    }

    @Bean
    @ConditionalOnProperty(name = "kin.knowledge.external-enabled", havingValue = "true")
    public KnowledgeSource externalHttpKnowledgeSource(KinKnowledgeProperties props, KnowledgeAdapterMetrics metrics) {
        KinKnowledgeProperties.Http http = props.getHttp();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(http.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        SourceConnectionGuard guard =
                new SourceConnectionGuard(Set.copyOf(props.getAllowedDomains()), http.isAllowLoopback());
        SecureHttpClient secureClient = new SecureHttpClient(
                client,
                guard,
                http.getSourceId(),
                http.getRequestTimeout(),
                http.getMaxResponseBytes(),
                http.getMaxRedirects(),
                Set.copyOf(http.getAllowedContentTypes()),
                http.getRetries(),
                http.getRetryBackoff(),
                metrics);
        return new HttpKnowledgeSourceAdapter(
                http.getSourceId(),
                http.getSourceName(),
                http.getBaseUrl(),
                secureClient,
                jsonItemsDecoder(new ObjectMapper()));
    }

    /**
     * Multi-fuente (ADR-021): un {@link CompositeKnowledgeSource} con un adapter
     * HTTP seguro por fuente configurada. Cada fuente declara su {@code format}
     * (decoder específico) y columnas de hecho. Se registra solo con
     * {@code external-enabled=true} y {@code sources[]} no vacío.
     */
    @Bean
    @ConditionalOnProperty(name = "kin.knowledge.external-enabled", havingValue = "true")
    public KnowledgeSource externalCompositeKnowledgeSource(
            KinKnowledgeProperties props, KnowledgeAdapterMetrics metrics) {
        List<KinKnowledgeProperties.SourceConfig> configs = props.getSources();
        if (configs == null || configs.isEmpty()) {
            return new CategoryAwareCompositeKnowledgeSource(List.of(), List.of());
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(props.getHttp().getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        SourceConnectionGuard guard = new SourceConnectionGuard(
                Set.copyOf(props.getAllowedDomains()), props.getHttp().isAllowLoopback());
        ObjectMapper mapper = new ObjectMapper();
        var adapters = new ArrayList<KnowledgeSource>();
        var categories = new ArrayList<List<String>>();
        var enabled = new ArrayList<Boolean>();
        var priorities = new ArrayList<Integer>();
        for (KinKnowledgeProperties.SourceConfig cfg : configs) {
            if (cfg == null
                    || cfg.getId() == null
                    || cfg.getId().isBlank()
                    || cfg.getBaseUrl() == null
                    || cfg.getBaseUrl().isBlank()) {
                continue;
            }
            SecureHttpClient secureClient = new SecureHttpClient(
                    client,
                    guard,
                    cfg.getId(),
                    props.getHttp().getRequestTimeout(),
                    props.getHttp().getMaxResponseBytes(),
                    props.getHttp().getMaxRedirects(),
                    Set.copyOf(props.getHttp().getAllowedContentTypes()),
                    props.getHttp().getRetries(),
                    props.getHttp().getRetryBackoff(),
                    metrics);
            adapters.add(new HttpKnowledgeSourceAdapter(
                    cfg.getId(),
                    cfg.getName(),
                    cfg.getBaseUrl(),
                    secureClient,
                    decoderFor(cfg, mapper),
                    cfg.getQueryParam()));
            categories.add(cfg.getCategories() == null ? List.of() : List.copyOf(cfg.getCategories()));
            enabled.add(cfg.isEnabled());
            priorities.add(cfg.getPriority());
        }
        return new CategoryAwareCompositeKnowledgeSource(adapters, categories, enabled, priorities);
    }

    @Bean
    @ConditionalOnProperty(name = "kin.knowledge.test-source.enabled", havingValue = "true")
    public KnowledgeSource controlledTestKnowledgeSource(KinKnowledgeProperties props) {
        KinKnowledgeProperties.TestSource test = props.getTestSource();
        return new ControlledTestKnowledgeSource(test.getSourceId(), test.getSourceName(), test.getCandidates());
    }

    /**
     * Selecciona el decoder según el formato declarado de la fuente. Cada
     * decoder traduce la respuesta real de la fuente a {@code HttpItem} crudos;
     * la validación siempre vive en el dominio ({@code SourceValidator}).
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>> decoderFor(
            KinKnowledgeProperties.SourceConfig cfg, ObjectMapper mapper) {
        KinKnowledgeProperties.SourceFormat format =
                cfg.getFormat() == null ? KinKnowledgeProperties.SourceFormat.JSON_ITEMS : cfg.getFormat();
        switch (format) {
            case WORLD_BANK_V2:
                return worldBankDecoder(mapper);
            case SODA_JSON:
                return sodaDecoder(mapper, cfg.getFactColumns(), cfg.getLandingPage());
            case INE_JSON:
                return ineDecoder(mapper, cfg.getLandingPage());
            case SDMX_JSON:
                return sdmxJsonDecoder(mapper, cfg.getLandingPage());
            case JSON_ITEMS:
            default:
                return jsonItemsDecoder(mapper);
        }
    }

    /**
     * Decoder INE España ({@code servicios.ine.es}): el cuerpo es un array de
     * series {@code [{COD, Nombre, Data:[{Fecha, Anyo, Valor}]}]}. Cada punto se
     * convierte en un hecho "{serie} ({año}): {valor}".
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>> ineDecoder(
            ObjectMapper mapper, String landingPage) {
        String base = landingPage == null ? "" : landingPage;
        return response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            try {
                JsonNode root = mapper.readTree(response.body());
                if (!root.isArray()) {
                    return List.of();
                }
                var out = new ArrayList<HttpKnowledgeSourceAdapter.HttpItem>();
                for (JsonNode series : root) {
                    if (!series.isObject()) {
                        continue;
                    }
                    String nombre = series.path("Nombre").asText("").trim();
                    JsonNode data = series.path("Data");
                    if (nombre.isBlank() || !data.isArray()) {
                        continue;
                    }
                    for (JsonNode point : data) {
                        String year = point.path("Anyo").asText("");
                        String value = plainText(point.path("Valor"));
                        if (value.isBlank()) {
                            continue;
                        }
                        String content = nombre + " (" + year + "): " + value;
                        String url = base + "#" + Integer.toHexString(content.hashCode());
                        out.add(new HttpKnowledgeSourceAdapter.HttpItem(content, url, OffsetDateTime.now()));
                    }
                }
                return List.copyOf(out);
            } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException ex) {
                return List.of();
            }
        };
    }

    /**
     * Decoder SDMX-JSON (p. ej. ECB {@code data-api.ecb.europa.eu}): lee la
     * estructura (dimensiones de serie y observación) y las observaciones de
     * {@code dataSets[0].series}. Cada observación se convierte en un hecho
     * "{serie} ({periodo}): {valor}".
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>> sdmxJsonDecoder(
            ObjectMapper mapper, String landingPage) {
        String base = landingPage == null ? "" : landingPage;
        return response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            try {
                JsonNode root = mapper.readTree(response.body());
                JsonNode structure = root.path("structure");
                JsonNode seriesDims = structure.path("dimensions").path("series");
                JsonNode obsDims = structure.path("dimensions").path("observation");
                JsonNode dataSets = root.path("dataSets");
                if (!dataSets.isArray() || dataSets.isEmpty()) {
                    return List.of();
                }
                List<String> periods = new ArrayList<>();
                if (obsDims.isArray() && !obsDims.isEmpty()) {
                    for (JsonNode v : obsDims.get(0).path("values")) {
                        periods.add(v.path("id").asText(""));
                    }
                }
                JsonNode series = dataSets.get(0).path("series");
                var out = new ArrayList<HttpKnowledgeSourceAdapter.HttpItem>();
                if (!series.isObject()) {
                    return List.of();
                }
                for (var entry : series.properties()) {
                    JsonNode observations = entry.getValue().path("observations");
                    if (!observations.isObject()) {
                        continue;
                    }
                    String serieName = resolveSeriesName(seriesDims, entry.getKey());
                    for (var obs : observations.properties()) {
                        int idx;
                        try {
                            idx = Integer.parseInt(obs.getKey());
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                        JsonNode valueNode =
                                obs.getValue().isArray() && obs.getValue().size() > 0
                                        ? obs.getValue().get(0)
                                        : obs.getValue();
                        String value = plainText(valueNode);
                        if (value.isBlank()) {
                            continue;
                        }
                        String period = idx >= 0 && idx < periods.size() ? periods.get(idx) : String.valueOf(idx);
                        String content = serieName + " (" + period + "): " + value;
                        String url = base + "#" + Integer.toHexString(content.hashCode());
                        out.add(new HttpKnowledgeSourceAdapter.HttpItem(content, url, OffsetDateTime.now()));
                    }
                }
                return List.copyOf(out);
            } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException ex) {
                return List.of();
            }
        };
    }

    /**
     * Resuelve el nombre legible de una serie SDMX a partir de su clave de
     * índices ({@code "0:0:0:0:0"} → nombres de las dimensiones de serie).
     */
    private static String resolveSeriesName(JsonNode seriesDims, String key) {
        if (!seriesDims.isArray() || key == null || key.isBlank()) {
            return key;
        }
        String[] parts = key.split(":");
        var names = new ArrayList<String>();
        for (int i = 0; i < parts.length && i < seriesDims.size(); i++) {
            try {
                int idx = Integer.parseInt(parts[i]);
                JsonNode values = seriesDims.get(i).path("values");
                if (idx >= 0 && idx < values.size()) {
                    String name = values.get(idx).path("name").asText("");
                    if (!name.isBlank()) {
                        names.add(name);
                    }
                }
            } catch (NumberFormatException ignored) {
                // índice no numérico: se ignora
            }
        }
        return names.isEmpty() ? key : String.join(" / ", names);
    }

    /**
     * Decoder de la API v2 del Banco Mundial:
     * {@code [ {page,lastupdated}, [ {indicator:{id,value}, country:{id,value},
     * countryiso3code, date, value, unit}, ... ] ]}. Cada fila se convierte en un
     * hecho con la forma "{indicador} — {país} ({año}): {valor} {unidad}".
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>>
            worldBankDecoder(ObjectMapper mapper) {
        return response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            try {
                JsonNode root = mapper.readTree(response.body());
                if (!root.isArray() || root.size() < 2) {
                    return List.of();
                }
                JsonNode meta = root.get(0);
                JsonNode rows = root.get(1);
                if (!rows.isArray()) {
                    return List.of();
                }
                OffsetDateTime publishedAt =
                        parsePublishedAt(meta.path("lastupdated").asText(""));
                var out = new ArrayList<HttpKnowledgeSourceAdapter.HttpItem>();
                for (JsonNode row : rows) {
                    String indicator = row.path("indicator").path("value").asText("");
                    String indicatorId = row.path("indicator").path("id").asText("");
                    String country = row.path("country").path("value").asText("");
                    String iso3 = row.path("countryiso3code").asText("");
                    String date = row.path("date").asText("");
                    String value = plainText(row.path("value"));
                    String unit = row.path("unit").asText("");
                    if (indicator.isBlank() || date.isBlank() || value.isBlank()) {
                        continue;
                    }
                    String content = indicator + " — " + country + " (" + date + "): " + value
                            + (unit.isBlank() ? "" : " " + unit);
                    String url = "https://api.worldbank.org/v2/country/" + iso3 + "/indicator/" + indicatorId
                            + "?format=json";
                    out.add(new HttpKnowledgeSourceAdapter.HttpItem(content, url, publishedAt));
                }
                return List.copyOf(out);
            } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException ex) {
                return List.of();
            }
        };
    }

    /**
     * Decoder SODA2 (Socrata, p. ej. datos.gov.co): el cuerpo es un array de
     * filas con columnas. Cada fila se convierte en un hecho concatenando las
     * {@code factColumns} configuradas ("columna: valor"). La URL del hecho es la
     * {@code landingPage} con un fragmento derivado del contenido (deduplicación
     * determinista entre filas de la misma fuente).
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>> sodaDecoder(
            ObjectMapper mapper, List<String> factColumns, String landingPage) {
        List<String> columns = factColumns == null ? List.of() : List.copyOf(factColumns);
        String base = landingPage == null ? "" : landingPage;
        return response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            try {
                JsonNode root = mapper.readTree(response.body());
                if (!root.isArray()) {
                    return List.of();
                }
                var out = new ArrayList<HttpKnowledgeSourceAdapter.HttpItem>();
                for (JsonNode row : root) {
                    if (!row.isObject()) {
                        continue;
                    }
                    var parts = new ArrayList<String>();
                    for (String column : columns) {
                        if (column == null || column.isBlank()) {
                            continue;
                        }
                        String value = row.path(column).asText("");
                        if (!value.isBlank()) {
                            parts.add(column + ": " + value);
                        }
                    }
                    if (parts.isEmpty()) {
                        continue;
                    }
                    String content = String.join(" | ", parts);
                    String url = base + "#" + Integer.toHexString(content.hashCode());
                    out.add(new HttpKnowledgeSourceAdapter.HttpItem(content, url, OffsetDateTime.now()));
                }
                return List.copyOf(out);
            } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException ex) {
                return List.of();
            }
        };
    }

    /**
     * Representación textual de un valor sin notación científica: los números se
     * formatean con {@link java.math.BigDecimal#toPlainString()} (el {@code asText}
     * de Jackson emite notación científica para valores grandes).
     */
    private static String plainText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        if (node.isNumber()) {
            return node.decimalValue().stripTrailingZeros().toPlainString();
        }
        return node.asText("").trim();
    }

    private static OffsetDateTime parsePublishedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return OffsetDateTime.now();
        }
        try {
            return java.time.LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay()
                    .atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            return OffsetDateTime.now();
        }
    }

    /**
     * Decoder por defecto del cuerpo JSON de una fuente autorizada. Contrato de
     * la fuente configurada: {@code {"items":[{"content","url","publishedAt"}]}}.
     * Fallos de parseo → lista vacía (offline-first).
     */
    static Function<HttpKnowledgeSourceAdapter.HttpResponse, List<HttpKnowledgeSourceAdapter.HttpItem>>
            jsonItemsDecoder(ObjectMapper mapper) {
        return response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            try {
                JsonNode root = mapper.readTree(response.body());
                JsonNode items = root.path("items");
                if (!items.isArray()) {
                    return List.of();
                }
                var out = new ArrayList<HttpKnowledgeSourceAdapter.HttpItem>();
                for (JsonNode item : items) {
                    out.add(new HttpKnowledgeSourceAdapter.HttpItem(
                            item.path("content").asText(""),
                            item.path("url").asText(""),
                            parseOffsetDateTime(item.path("publishedAt").asText(""))));
                }
                return List.copyOf(out);
            } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException ex) {
                return List.of();
            }
        };
    }

    private static OffsetDateTime parseOffsetDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
