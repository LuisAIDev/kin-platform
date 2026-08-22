package com.kinplatform.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
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
 * Prueba de concepto del mapeo categoría → fuentes (ADR-024) contra RED REAL:
 * un proyecto creado en AGROINDUSTRIA, FINTECH o SALUD hace que el
 * {@code CategoryAwareCompositeKnowledgeSource} consulte solo las fuentes
 * pertinentes (contexto general + las de su categoría), nunca las de otras.
 * Test <em>gated</em> por {@code KIN_TEST_REAL_NETWORK=true}.
 */
@EnabledIfEnvironmentVariable(named = "KIN_TEST_REAL_NETWORK", matches = "true")
class KnowledgeCategoryMappingTest {

    @Test
    void agroindustria_consultaSoloFuentesAgroYContextoGeneral() {
        var used = run("AGROINDUSTRIA", "Tostadora y exportadora de café de especialidad");
        assertTrue(used.contains("insumos-agricolas"), "debe consultar insumos agrícolas: " + used);
        assertTrue(used.contains("dane-exportaciones-cafe"), "debe consultar exportaciones de café: " + used);
        assertTrue(used.contains("finagro-desembolsos"), "debe consultar desembolsos Finagro: " + used);
        assertFalse(used.contains("superfinanciera-tasas"), "no debe consultar tasas (FINTECH): " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud (SALUD): " + used);
    }

    @Test
    void fintech_consultaFuentesFinancieras_noLasDeAgro() {
        var used = run("FINTECH", "Neobanco de microcréditos para pymes");
        assertTrue(used.contains("superfinanciera-tasas"), "debe consultar tasas: " + used);
        assertTrue(used.contains("superfinanciera-cartera"), "debe consultar cartera: " + used);
        assertFalse(used.contains("insumos-agricolas"), "no debe consultar insumos agrícolas: " + used);
        assertFalse(used.contains("dane-exportaciones-cafe"), "no debe consultar exportaciones de café: " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud: " + used);
    }

    @Test
    void salud_consultaFuentesDeSalud_noLasFinancieras() {
        var used = run("SALUD", "Clínica de atención ambulatoria");
        assertTrue(used.contains("saludatos-ths"), "debe consultar Saludatos (talento humano): " + used);
        assertFalse(used.contains("superfinanciera-tasas"), "no debe consultar tasas: " + used);
        assertFalse(used.contains("insumos-agricolas"), "no debe consultar insumos agrícolas: " + used);
    }

    @Test
    void logistica_consultaFuentesDeLogistica_conReusoDeExportaciones() {
        var used = run("LOGISTICA", "Operador de transporte aéreo de carga perecedera");
        assertTrue(used.contains("transporte-aereo-colombia"), "debe consultar transporte aéreo: " + used);
        assertTrue(used.contains("dane-exportaciones-cafe"), "reuso: exportaciones informan logística: " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud: " + used);
        assertFalse(used.contains("superfinanciera-tasas"), "no debe consultar tasas: " + used);
    }

    @Test
    void tecnologia_consultaFuentesMinTIC() {
        var used = run("TECNOLOGIA", "Startup de software de inteligencia artificial");
        assertTrue(used.contains("mintic-internet-fijo"), "debe consultar internet fijo: " + used);
        assertTrue(used.contains("mintic-internet-movil"), "debe consultar internet móvil: " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud: " + used);
        assertFalse(used.contains("minciencias-proyectos"), "no debe consultar investigación: " + used);
    }

    @Test
    void investigacion_consultaFuentesMinCiencias() {
        var used = run("INVESTIGACION", "Proyecto de investigación en biotecnología");
        assertTrue(used.contains("minciencias-proyectos"), "debe consultar proyectos MinCiencias: " + used);
        assertFalse(used.contains("mintic-internet-fijo"), "no debe consultar internet: " + used);
        assertFalse(used.contains("insumos-agricolas"), "no debe consultar insumos: " + used);
    }

    @Test
    void gobierno_consultaFuentesSECOP() {
        var used = run("GOBIERNO", "Consultoría B2G para entidades públicas");
        assertTrue(used.contains("secop-origen-recursos"), "debe consultar SECOP: " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud: " + used);
        assertFalse(used.contains("mintic-internet-fijo"), "no debe consultar MinTIC: " + used);
    }

    @Test
    void servicios_consultaConfecamaras() {
        var used = run("SERVICIOS", "Empresa de servicios de outsourcing");
        assertTrue(used.contains("confecamaras-empresas"), "reuso: registro mercantil aplica a servicios: " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud: " + used);
        assertFalse(used.contains("minciencias-proyectos"), "no debe consultar investigación: " + used);
    }

    @Test
    void marketingDigital_consultaMinTIC() {
        var used = run("MARKETING_DIGITAL", "Agencia de marketing digital y publicidad");
        assertTrue(used.contains("mintic-internet-fijo"), "reuso: penetración digital: " + used);
        assertTrue(used.contains("mintic-internet-movil"), "reuso: tráfico móvil: " + used);
        assertFalse(used.contains("secop-origen-recursos"), "no debe consultar SECOP: " + used);
        assertFalse(used.contains("saludatos-ths"), "no debe consultar salud: " + used);
    }

    @Test
    void gastronomia_consultaInsumos() {
        var used = run("GASTRONOMIA", "Restaurante de comida colombiana");
        assertTrue(used.contains("insumos-agricolas"), "reuso: precios de insumos alimentarios: " + used);
        assertFalse(used.contains("secop-origen-recursos"), "no debe consultar SECOP: " + used);
        assertFalse(used.contains("minciencias-proyectos"), "no debe consultar investigación: " + used);
    }

    private static Set<String> run(String category, String topic) {
        var props = props();
        KnowledgeAdapterMetrics metrics = new KnowledgeAdapterMetrics(new SimpleMeterRegistry());
        KnowledgeSource composite =
                new KnowledgeHttpAutoConfiguration().externalCompositeKnowledgeSource(props, metrics);
        KnowledgeEngine engine = new KnowledgeEngine(new KnowledgeGateway(
                new SourceRegistry(List.of(composite)),
                new SourceValidator(Set.copyOf(props.getAllowedDomains()), null, Set.of("application/json"))));
        KnowledgeRequest request = new KnowledgeRequest(
                topic, Set.of(), List.of("mercado", "colombia"), 30, Duration.ofDays(365), category);
        long start = System.currentTimeMillis();
        KnowledgeResult result = engine.evaluate(KnowledgeInput.of(request));
        System.out.println("== CAT [" + category + "] fuentes=" + result.sourcesUsed() + " facts=" + result.factCount()
                + " en " + (System.currentTimeMillis() - start) + " ms");
        result.facts().stream().limit(4).forEach(f -> System.out.println("     [fact] " + f.claim()));
        assertFalse(result.isEmpty(), "la categoría " + category + " debe producir hechos");
        return Set.copyOf(result.sourcesUsed());
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
}
