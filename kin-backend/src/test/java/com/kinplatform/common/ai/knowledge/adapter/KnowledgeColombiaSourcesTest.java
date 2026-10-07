package com.kinplatform.common.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.knowledge.KnowledgeInput;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import com.kinplatform.common.knowledge.engine.KnowledgeGateway;
import com.kinplatform.common.knowledge.engine.SourceRegistry;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Cobertura exhaustiva de fuentes oficiales de Colombia (ADR-023 §Colombia)
 * contra RED REAL: cada dataset nuevo de {@code application-staging.yml} se
 * prueba individualmente y se combinan en un escenario de comercio exterior
 * (TRM + exportaciones de café + tasas de interés + desembolsos de
 * financiamiento). Test <em>gated</em> por {@code KIN_TEST_REAL_NETWORK=true}.
 */
@EnabledIfEnvironmentVariable(named = "KIN_TEST_REAL_NETWORK", matches = "true")
class KnowledgeColombiaSourcesTest {

    @Test
    void cadaFuenteColombianaNueva_produceHechosReales() {
        var props = loadStagingProperties();
        var selected = props.getSources().stream()
                .filter(s -> s.getRegion().equals("COL") && s.getLevel() == 2)
                .toList();
        assertTrue(selected.size() >= 5, "debe haber al menos 5 datasets colombianos");
        for (var cfg : selected) {
            KnowledgeResult result = run(List.of(cfg), "Proyecto de análisis Colombia", 5);
            System.out.println("== CO [" + cfg.getId() + "] facts=" + result.factCount() + " => "
                    + (result.isEmpty() ? "VACÍO" : "OK"));
            result.facts().stream().limit(2).forEach(f -> System.out.println("     [fact] " + f.claim()));
            assertFalse(result.isEmpty(), "la fuente " + cfg.getId() + " debería producir hechos reales");
        }
    }

    @Test
    void escenarioComercioExterior_combinado_usaVariasFuentes() {
        var props = loadStagingProperties();
        List<String> ids = List.of(
                "trm-col", "dane-exportaciones-cafe", "superfinanciera-tasas", "finagro-desembolsos", "worldbank-pib");
        var selected =
                props.getSources().stream().filter(s -> ids.contains(s.getId())).toList();
        assertTrue(selected.size() == ids.size(), "faltan fuentes en la config staging: " + ids);

        KnowledgeResult result = run(selected, "Emprendimiento de exportación de café especial a EE.UU.", 40);
        System.out.println(
                "== CO ESCENARIO COMBINADO fuentes=" + result.sourcesUsed() + " facts=" + result.factCount());
        result.facts().forEach(f -> System.out.println("     [fact] " + f.claim()));
        assertFalse(result.isEmpty(), "el escenario combinado debe producir hechos de varias fuentes");
        assertTrue(result.sourcesUsed().contains("dane-exportaciones-cafe"));
    }

    private static KnowledgeResult run(List<KinKnowledgeProperties.SourceConfig> selected, String topic, int limit) {
        KnowledgeAdapterMetrics metrics = new KnowledgeAdapterMetrics(new SimpleMeterRegistry());
        var composite = new ArrayList<KnowledgeSource>();
        for (var cfg : selected) {
            composite.add(new HttpKnowledgeSourceAdapter(
                    cfg.getId(),
                    cfg.getName(),
                    cfg.getBaseUrl(),
                    secureClient(metrics, cfg.getId()),
                    KnowledgeHttpAutoConfiguration.decoderFor(cfg, new com.fasterxml.jackson.databind.ObjectMapper()),
                    cfg.getQueryParam()));
        }
        Set<String> domains = Set.copyOf(props().getAllowedDomains());
        KnowledgeEngine engine = new KnowledgeEngine(new KnowledgeGateway(
                new SourceRegistry(List.of(new CompositeKnowledgeSource(composite))),
                new SourceValidator(domains, null, Set.of("application/json"))));
        KnowledgeRequest request =
                new KnowledgeRequest(topic, Set.of(), List.of("mercado", "colombia"), limit, Duration.ofDays(365));
        long start = System.currentTimeMillis();
        KnowledgeResult result = engine.evaluate(KnowledgeInput.of(request));
        System.out.println("== CO [" + topic + "] " + (System.currentTimeMillis() - start) + " ms");
        return result;
    }

    private static SecureHttpClient secureClient(KnowledgeAdapterMetrics metrics, String sourceId) {
        return new SecureHttpClient(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                        .build(),
                new SourceConnectionGuard(Set.copyOf(props().getAllowedDomains()), false),
                sourceId,
                Duration.ofSeconds(15),
                262_144,
                3,
                Set.of("application/json"),
                1,
                Duration.ofMillis(250),
                metrics);
    }

    private static final Map<String, Object> RAW = loadRaw();
    private static KinKnowledgeProperties cached;

    private static KinKnowledgeProperties props() {
        if (cached == null) {
            Map<String, Object> map = new HashMap<>();
            for (var entry : RAW.entrySet()) {
                Object value = entry.getValue();
                map.put(entry.getKey(), (value instanceof String) ? resolve((String) value) : value);
            }
            cached = new Binder(new MapConfigurationPropertySource(map))
                    .bind("kin.knowledge", Bindable.of(KinKnowledgeProperties.class))
                    .get();
        }
        return cached;
    }

    private static Map<String, Object> loadRaw() {
        try {
            var loader = new org.springframework.boot.env.YamlPropertySourceLoader();
            var sources = loader.load("staging", new ClassPathResource("application-staging.yml"));
            Map<String, Object> out = new HashMap<>();
            for (var source : sources) {
                for (var entry : ((java.util.Map<String, Object>) source.getSource()).entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof org.springframework.boot.origin.OriginTrackedValue tracked) {
                        value = tracked.getValue();
                    }
                    out.put(entry.getKey(), value);
                }
            }
            return out;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo cargar application-staging.yml", ex);
        }
    }

    private static String resolve(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^\\$\\{([^:}]+):(.*)\\}$").matcher(value.trim());
        if (m.matches()) {
            String env = System.getenv(m.group(1));
            return env == null ? m.group(2) : env;
        }
        return value;
    }

    private static KinKnowledgeProperties loadStagingProperties() {
        return props();
    }
}


