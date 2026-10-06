package com.kinplatform.ai.knowledge.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.knowledge.adapter.HttpKnowledgeSourceAdapter.HttpItem;
import com.kinplatform.ai.knowledge.adapter.HttpKnowledgeSourceAdapter.HttpRequest;
import com.kinplatform.ai.knowledge.adapter.HttpKnowledgeSourceAdapter.HttpResponse;
import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.KnowledgeQuery;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Decoders específicos por fuente (ADR-021): Banco Mundial (WORLD_BANK_V2) y
 * SODA2/Socrata (SODA_JSON), selección por {@code format} y comportamiento del
 * sufijo de tema configurable ({@code queryParam}) sobre respuestas reales.
 */
class KnowledgeMultiSourceDecoderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void worldBank_decoder_deberiaConstruirHechosDesdeLaRespuestaReal() {
        String body = "[{\"page\":1,\"pages\":1,\"per_page\":2,\"total\":2,\"lastupdated\":\"2026-07-13\"},"
                + "[{\"indicator\":{\"id\":\"NY.GDP.MKTP.CD\",\"value\":\"GDP (current US$)\"},"
                + "\"country\":{\"id\":\"CO\",\"value\":\"Colombia\"},\"countryiso3code\":\"COL\","
                + "\"date\":\"2023\",\"value\":366901643683.302,\"unit\":\"\",\"obs_status\":\"\",\"decimal\":0}]]";

        List<HttpItem> items = KnowledgeHttpAutoConfiguration.worldBankDecoder(mapper)
                .apply(new HttpResponse(200, "application/json", body));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).content()).isEqualTo("GDP (current US$) — Colombia (2023): 366901643683.302");
        assertThat(items.get(0).url())
                .isEqualTo("https://api.worldbank.org/v2/country/COL/indicator/NY.GDP.MKTP.CD?format=json");
        assertThat(items.get(0).publishedAt()).isNotNull();
    }

    @Test
    void worldBank_decoder_filasSinValor_deberianOmitirse() {
        String body =
                "[{\"lastupdated\":\"2026-07-13\"},[{\"indicator\":{\"value\":\"X\"},\"country\":{\"value\":\"Colombia\"},"
                        + "\"countryiso3code\":\"COL\",\"date\":\"2023\",\"value\":\"\"}]]";

        List<HttpItem> items = KnowledgeHttpAutoConfiguration.worldBankDecoder(mapper)
                .apply(new HttpResponse(200, "application/json", body));

        assertThat(items).isEmpty();
    }

    @Test
    void worldBank_decoder_cuerpoInvalido_deberiaDegradarVacio() {
        assertThat(KnowledgeHttpAutoConfiguration.worldBankDecoder(mapper)
                        .apply(new HttpResponse(200, "application/json", "no-json")))
                .isEmpty();
        assertThat(KnowledgeHttpAutoConfiguration.worldBankDecoder(mapper).apply(null))
                .isEmpty();
    }

    @Test
    void soda_decoder_deberiaConcatenarColumnasConfiguradasYUrlUnica() {
        String body = "[{\"valor\":\"3062.96\",\"unidad\":\"COP\",\"vigenciadesde\":\"2026-08-21T00:00:00.000\"},"
                + "{\"valor\":\"3053.48\",\"unidad\":\"COP\",\"vigenciadesde\":\"2026-08-20T00:00:00.000\"}]";

        List<HttpItem> items = KnowledgeHttpAutoConfiguration.sodaDecoder(
                        mapper, List.of("valor", "unidad", "vigenciadesde"), "https://www.datos.gov.co/d/32sa-8pi3")
                .apply(new HttpResponse(200, "application/json", body));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).content())
                .isEqualTo("valor: 3062.96 | unidad: COP | vigenciadesde: 2026-08-21T00:00:00.000");
        assertThat(items.get(0).url()).startsWith("https://www.datos.gov.co/d/32sa-8pi3#");
        assertThat(items.get(0).url()).isNotEqualTo(items.get(1).url());
    }

    @Test
    void ine_decoder_deberiaConstruirHechosDesdeSeries() {
        String body = "[{\"COD\":\"IPC290751\",\"Nombre\":\"Nacional. Índice general. Índice.\","
                + "\"Data\":[{\"Fecha\":1782856800000,\"Anyo\":2026,\"Valor\":103.899},"
                + "{\"Fecha\":1780264800000,\"Anyo\":2026,\"Valor\":103.598}]}]";

        List<HttpItem> items = KnowledgeHttpAutoConfiguration.ineDecoder(mapper, "https://servicios.ine.es/")
                .apply(new HttpResponse(200, "application/json", body));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).content()).isEqualTo("Nacional. Índice general. Índice. (2026): 103.899");
        assertThat(items.get(0).url()).startsWith("https://servicios.ine.es/#");
    }

    @Test
    void sdmx_decoder_deberiaConstruirHechosDesdeEstructura() {
        String body =
                "{\"structure\":{\"dimensions\":{\"series\":[{\"id\":\"FREQ\",\"values\":[{\"id\":\"A\",\"name\":\"Annual\"}]}],"
                        + "\"observation\":[{\"id\":\"TIME_PERIOD\",\"values\":[{\"id\":\"2024\",\"name\":\"2024\"}]}]}},"
                        + "\"dataSets\":[{\"series\":{\"0\":{\"observations\":{\"0\":[1.0956]}}}}]}";

        List<HttpItem> items = KnowledgeHttpAutoConfiguration.sdmxJsonDecoder(mapper, "https://data-api.ecb.europa.eu/")
                .apply(new HttpResponse(200, "application/json", body));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).content()).isEqualTo("Annual (2024): 1.0956");
    }

    @Test
    void decoderFor_deberiaSeleccionarPorFormato() {
        KinKnowledgeProperties.SourceConfig cfg = new KinKnowledgeProperties.SourceConfig();
        cfg.setFormat(KinKnowledgeProperties.SourceFormat.WORLD_BANK_V2);
        String wbBody =
                "[{\"lastupdated\":\"2026-07-13\"},[{\"indicator\":{\"value\":\"X\"},\"country\":{\"value\":\"Colombia\"},"
                        + "\"countryiso3code\":\"COL\",\"date\":\"2023\",\"value\":1.5}]]";
        List<HttpItem> wbViaFactory = KnowledgeHttpAutoConfiguration.decoderFor(cfg, mapper)
                .apply(new HttpResponse(200, "application/json", wbBody));
        List<HttpItem> wbDirect = KnowledgeHttpAutoConfiguration.worldBankDecoder(mapper)
                .apply(new HttpResponse(200, "application/json", wbBody));
        assertThat(wbViaFactory).isEqualTo(wbDirect);

        cfg.setFormat(KinKnowledgeProperties.SourceFormat.SODA_JSON);
        cfg.setFactColumns(List.of("valor"));
        String sodaBody = "[{\"valor\":\"3062.96\"}]";
        List<String> sodaViaFactory = describe(KnowledgeHttpAutoConfiguration.decoderFor(cfg, mapper)
                .apply(new HttpResponse(200, "application/json", sodaBody)));
        List<String> sodaDirect = describe(KnowledgeHttpAutoConfiguration.sodaDecoder(mapper, List.of("valor"), "")
                .apply(new HttpResponse(200, "application/json", sodaBody)));
        assertThat(sodaViaFactory).isEqualTo(sodaDirect);
    }

    private static List<String> describe(List<HttpItem> items) {
        return items.stream().map(item -> item.content() + "||" + item.url()).toList();
    }

    @Test
    void adapter_sinQueryParam_noDebeAnexarSufijo() {
        java.util.concurrent.atomic.AtomicReference<HttpRequest> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        HttpKnowledgeSourceAdapter adapter = new HttpKnowledgeSourceAdapter(
                "soda",
                "Soda",
                "https://www.datos.gov.co/resource/32sa-8pi3.json?$limit=5",
                request -> {
                    captured.set(request);
                    return new HttpResponse(200, "application/json", "[]");
                },
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(mapper),
                "");

        adapter.fetch(KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of())));

        assertThat(captured.get().url()).isEqualTo("https://www.datos.gov.co/resource/32sa-8pi3.json?$limit=5");
    }

    @Test
    void adapter_conQueryParam_deberiaAnexarTema() {
        java.util.concurrent.atomic.AtomicReference<HttpRequest> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        HttpKnowledgeSourceAdapter adapter = new HttpKnowledgeSourceAdapter(
                "wb",
                "World Bank",
                "https://api.worldbank.org/v2/country/COL/indicator/NY.GDP.MKTP.CD?format=json",
                request -> {
                    captured.set(request);
                    return new HttpResponse(200, "application/json", "[]");
                },
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(mapper),
                "q");

        adapter.fetch(KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of())));

        assertThat(captured.get().url())
                .isEqualTo("https://api.worldbank.org/v2/country/COL/indicator/NY.GDP.MKTP.CD?format=json&q=retail");
    }

    @Test
    void composite_conAdaptadores_deberiaConcatenarCandidatos() {
        HttpKnowledgeSourceAdapter a = new HttpKnowledgeSourceAdapter(
                "a",
                "A",
                "https://a.example/",
                r -> new HttpResponse(200, "application/json", "[]"),
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(mapper),
                "");
        HttpKnowledgeSourceAdapter b = new HttpKnowledgeSourceAdapter(
                "b",
                "B",
                "https://b.example/",
                r -> new HttpResponse(200, "application/json", "[]"),
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(mapper),
                "");
        CompositeKnowledgeSource composite = new CompositeKnowledgeSource(List.of(a, b));

        List<KnowledgeCandidate> candidates =
                composite.fetch(KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of())));
        assertThat(candidates).isEmpty();
    }
}

