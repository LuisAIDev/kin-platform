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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Allowlist global por niveles (ADR-023) contra RED REAL: selecciona fuentes por
 * región del proyecto (Nivel 1 global + Nivel 2 nacional) y ejecuta el
 * {@link KnowledgeEngine} para proyectos en Colombia, España, México, EE.UU. y
 * un país sin fuente nacional (Ecuador → respaldo Nivel 1). Test <em>gated</em>
 * por {@code KIN_TEST_REAL_NETWORK=true}.
 */
@EnabledIfEnvironmentVariable(named = "KIN_TEST_REAL_NETWORK", matches = "true")
class KnowledgeGlobalAllowlistTest {

    private static final Set<String> DOMAINS =
            Set.of("api.worldbank.org", "www.datos.gov.co", "servicios.ine.es", "data-api.ecb.europa.eu");

    /** Selección por región: Nivel 1 (global o del país) + Nivel 2 del país. */
    private static List<KinKnowledgeProperties.SourceConfig> select(
            List<KinKnowledgeProperties.SourceConfig> all, String country) {
        var selected = new ArrayList<KinKnowledgeProperties.SourceConfig>();
        for (var s : all) {
            if (s.getLevel() == 2) {
                if (s.getRegion().equalsIgnoreCase(country)) {
                    selected.add(s);
                }
            } else if (s.getRegion().equalsIgnoreCase(country) || s.getRegion().equalsIgnoreCase("GLOBAL")) {
                selected.add(s);
            }
        }
        return selected;
    }

    private static KinKnowledgeProperties.SourceConfig wb(String country) {
        KinKnowledgeProperties.SourceConfig s = new KinKnowledgeProperties.SourceConfig();
        s.setId("worldbank-" + country.toLowerCase());
        s.setName("Banco Mundial — PIB " + country);
        s.setBaseUrl("https://api.worldbank.org/v2/country/" + country
                + "/indicator/NY.GDP.MKTP.CD?format=json&per_page=5&date=2020:2024");
        s.setFormat(KinKnowledgeProperties.SourceFormat.WORLD_BANK_V2);
        s.setQueryParam("");
        s.setLevel(1);
        s.setRegion(country);
        s.setCategories(List.of("ECONOMY"));
        return s;
    }

    private static KinKnowledgeProperties.SourceConfig ecb() {
        KinKnowledgeProperties.SourceConfig s = new KinKnowledgeProperties.SourceConfig();
        s.setId("ecb-usdeur");
        s.setName("ECB — Tipo de cambio USD/EUR");
        s.setBaseUrl(
                "https://data-api.ecb.europa.eu/service/data/EXR/D.USD.EUR.SP00.A?format=jsondata&startPeriod=2023");
        s.setFormat(KinKnowledgeProperties.SourceFormat.SDMX_JSON);
        s.setQueryParam("");
        s.setLandingPage("https://data-api.ecb.europa.eu/");
        s.setLevel(1);
        s.setRegion("GLOBAL");
        s.setCategories(List.of("ECONOMY"));
        return s;
    }

    private static KinKnowledgeProperties.SourceConfig soda(
            String id, String name, String url, String landing, List<String> columns, String country) {
        KinKnowledgeProperties.SourceConfig s = new KinKnowledgeProperties.SourceConfig();
        s.setId(id);
        s.setName(name);
        s.setBaseUrl(url);
        s.setFormat(KinKnowledgeProperties.SourceFormat.SODA_JSON);
        s.setQueryParam("");
        s.setFactColumns(columns);
        s.setLandingPage(landing);
        s.setLevel(2);
        s.setRegion(country);
        s.setCategories(List.of("ECONOMY"));
        return s;
    }

    private static KinKnowledgeProperties.SourceConfig ine() {
        KinKnowledgeProperties.SourceConfig s = new KinKnowledgeProperties.SourceConfig();
        s.setId("ine-ipc");
        s.setName("INE España — IPC nacional");
        s.setBaseUrl("https://servicios.ine.es/wstempus/js/ES/DATOS_TABLA/24077?nult=3");
        s.setFormat(KinKnowledgeProperties.SourceFormat.INE_JSON);
        s.setQueryParam("");
        s.setLandingPage("https://servicios.ine.es/");
        s.setLevel(2);
        s.setRegion("ESP");
        s.setCategories(List.of("ECONOMY"));
        return s;
    }

    private static List<KinKnowledgeProperties.SourceConfig> registry() {
        var all = new ArrayList<KinKnowledgeProperties.SourceConfig>();
        all.add(ecb());
        for (String cc : new String[] {"COL", "ESP", "MEX", "USA", "ECU"}) {
            all.add(wb(cc));
        }
        all.add(soda(
                "trm-col",
                "Datos Abiertos Colombia — TRM",
                "https://www.datos.gov.co/resource/32sa-8pi3.json?$limit=5&$order=vigenciadesde%20DESC",
                "https://www.datos.gov.co/d/32sa-8pi3",
                List.of("valor", "unidad", "vigenciadesde"),
                "COL"));
        all.add(soda(
                "pib-departamental",
                "Datos Abiertos Colombia — PIB departamental (DANE)",
                "https://www.datos.gov.co/resource/kgyi-qc7j.json?$limit=100",
                "https://www.datos.gov.co/d/kgyi-qc7j",
                List.of("a_o", "departamento", "actividad", "valor_miles_de_millones_de"),
                "COL"));
        all.add(ine());
        return all;
    }

    private static KnowledgeResult run(List<KinKnowledgeProperties.SourceConfig> selected, String topic, int limit) {
        KnowledgeAdapterMetrics metrics = new KnowledgeAdapterMetrics(new SimpleMeterRegistry());
        KnowledgeHttpAutoConfiguration config = new KnowledgeHttpAutoConfiguration();
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
        KnowledgeEngine engine = new KnowledgeEngine(new KnowledgeGateway(
                new SourceRegistry(List.of(new CompositeKnowledgeSource(composite))),
                new SourceValidator(DOMAINS, null, Set.of("application/json"))));
        KnowledgeRequest request =
                new KnowledgeRequest(topic, Set.of(), List.of("mercado", "país"), limit, Duration.ofDays(365));
        long start = System.currentTimeMillis();
        KnowledgeResult result = engine.evaluate(KnowledgeInput.of(request));
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("== GLOBAL [" + topic + "] fuentes=" + result.sourcesUsed() + " facts=" + result.factCount()
                + " en " + elapsed + " ms");
        result.facts().stream().limit(3).forEach(f -> System.out.println("   [fact] " + f.claim()));
        return result;
    }

    private static SecureHttpClient secureClient(KnowledgeAdapterMetrics metrics, String sourceId) {
        return new SecureHttpClient(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                        .build(),
                new SourceConnectionGuard(DOMAINS, false),
                sourceId,
                Duration.ofSeconds(15),
                262_144,
                3,
                Set.of("application/json"),
                1,
                Duration.ofMillis(250),
                metrics);
    }

    @Test
    void ecbSolo_aTravesDelGateway_deberiaProducirHechos() {
        KnowledgeResult result = run(List.of(ecb()), "Tipo de cambio global", 5);
        assertFalse(result.isEmpty());
    }

    @Test
    void colombia_produceHechosDeNivel1YFuenteNacional() {
        var selected = select(registry(), "COL");
        assertTrue(selected.stream()
                .anyMatch(s -> s.getLevel() == 2 && s.getRegion().equals("COL")));
        KnowledgeResult result = run(selected, "Café de especialidad Colombia", 20);
        assertFalse(result.isEmpty());
    }

    @Test
    void espana_produceHechosDeNivel1YFuenteINE() {
        var selected = select(registry(), "ESP");
        assertTrue(selected.stream().anyMatch(s -> s.getId().equals("ine-ipc")));
        KnowledgeResult result = run(selected, "Startup de software en España", 20);
        assertFalse(result.isEmpty());
    }

    @Test
    void mexico_produceHechosDeNivel1_sinFuenteNacional() {
        var selected = select(registry(), "MEX");
        assertTrue(selected.stream().noneMatch(s -> s.getLevel() == 2), "México aún sin fuente nacional verificada");
        KnowledgeResult result = run(selected, "Comercio electrónico en México", 20);
        assertFalse(result.isEmpty(), "Nivel 1 (Banco Mundial) debe cubrir México");
    }

    @Test
    void eeuu_produceHechosDeNivel1() {
        var selected = select(registry(), "USA");
        assertTrue(selected.stream().noneMatch(s -> s.getLevel() == 2), "EE.UU. aún sin fuente nacional integrada");
        KnowledgeResult result = run(selected, "Empresa de alimentos en EE.UU.", 20);
        assertFalse(result.isEmpty(), "Nivel 1 debe cubrir EE.UU.");
    }

    @Test
    void paisSinNivel2_ecuador_recurreANivel1() {
        var selected = select(registry(), "ECU");
        assertTrue(selected.stream().noneMatch(s -> s.getLevel() == 2), "Ecuador no tiene fuente nacional (Nivel 2)");
        assertTrue(selected.stream().anyMatch(s -> s.getLevel() == 1), "Debe quedar el respaldo Nivel 1");
        KnowledgeResult result = run(selected, "Cacao orgánico en Ecuador", 20);
        assertFalse(result.isEmpty(), "El respaldo Nivel 1 debe producir hechos sin fallar");
    }

    @Test
    void fuenteInalcanzable_degradaSinRomperElAnalisis() {
        var selected = new ArrayList<>(select(registry(), "COL"));
        KinKnowledgeProperties.SourceConfig broken = new KinKnowledgeProperties.SourceConfig();
        broken.setId("fuente-rota");
        broken.setName("Fuente rota");
        broken.setBaseUrl("https://httpbin.org/status/500");
        broken.setFormat(KinKnowledgeProperties.SourceFormat.WORLD_BANK_V2);
        broken.setQueryParam("");
        broken.setLevel(2);
        broken.setRegion("COL");
        selected.add(broken);

        KnowledgeResult result = run(selected, "Café de especialidad Colombia", 20);
        assertFalse(result.isEmpty(), "la fuente rota no debe impedir los hechos de las fuentes sanas");
    }
}
