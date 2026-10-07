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
 * Verificación de RED REAL con la configuración REAL de staging
 * ({@code application-staging.yml}, ADR-021): construye el composite multi-fuente
 * con el cableado de producción ({@code KnowledgeHttpAutoConfiguration}) y
 * ejecuta el {@link KnowledgeEngine} contra las fuentes aprobadas
 * (Banco Mundial + datos.gov.co). Test <em>gated</em> por
 * {@code KIN_TEST_REAL_NETWORK=true} (requiere salida a Internet).
 */
@EnabledIfEnvironmentVariable(named = "KIN_TEST_REAL_NETWORK", matches = "true")
class KnowledgeStagingSourcesTest {

    @Test
    void stagingConfig_real_deberiaProducirHechosDeFuentesOficiales() {
        KinKnowledgeProperties props = loadStagingProperties();
        assertTrue(props.isExternalEnabled());
        assertTrue(props.getAllowedDomains().contains("api.worldbank.org"));
        assertTrue(props.getAllowedDomains().contains("www.datos.gov.co"));
        assertTrue(props.getAllowedDomains().contains("data-api.ecb.europa.eu"));
        assertTrue(props.getAllowedDomains().contains("servicios.ine.es"));
        assertTrue(props.getSources().size() >= 13);

        KnowledgeAdapterMetrics metrics = new KnowledgeAdapterMetrics(new SimpleMeterRegistry());
        KnowledgeHttpAutoConfiguration config = new KnowledgeHttpAutoConfiguration();
        KnowledgeSource composite = config.externalCompositeKnowledgeSource(props, metrics);

        SourceRegistry registry = new SourceRegistry(List.of(composite));
        SourceValidator validator = new SourceValidator(Set.copyOf(props.getAllowedDomains()), null, Set.of());
        KnowledgeEngine engine = new KnowledgeEngine(new KnowledgeGateway(registry, validator));

        KnowledgeRequest request = new KnowledgeRequest(
                "café de especialidad",
                Set.of(),
                List.of("mercado", "colombia", "exportación"),
                KnowledgeRequest.DEFAULT_LIMIT,
                Duration.ofDays(365));

        long start = System.currentTimeMillis();
        KnowledgeResult result = engine.evaluate(KnowledgeInput.of(request));
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("== STAGING RED REAL: " + elapsed + " ms, facts=" + result.factCount() + ", sources="
                + result.sourcesUsed());
        result.facts()
                .forEach(fact -> System.out.println(
                        "   [fact] trust=" + fact.trust() + " url=" + fact.url() + " :: " + fact.claim()));
        System.out.println("== explanation=" + result.explanation());

        assertFalse(result.isEmpty(), "la configuración de staging debería producir hechos reales");
    }

    private static final Map<String, Object> RAW = loadRaw();

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

    /** Resuelve ${VAR:default} con el entorno o el default (el Binder no resuelve placeholders). */
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
        Map<String, Object> map = new HashMap<>();
        for (var entry : RAW.entrySet()) {
            Object value = entry.getValue();
            map.put(entry.getKey(), (value instanceof String) ? resolve((String) value) : value);
        }
        return new Binder(new MapConfigurationPropertySource(map))
                .bind("kin.knowledge", Bindable.of(KinKnowledgeProperties.class))
                .get();
    }
}


