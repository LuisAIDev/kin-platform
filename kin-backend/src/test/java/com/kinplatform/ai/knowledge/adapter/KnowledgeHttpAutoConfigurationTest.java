package com.kinplatform.ai.knowledge.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifica el cableado condicional (offline-first) de los adaptadores de
 * conocimiento (ADR-021) sin levantar el contexto completo.
 */
class KnowledgeHttpAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(KnowledgeHttpAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void porDefecto_noDebeRegistrarFuentesExternas() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(KnowledgeAdapterMetrics.class);
            assertThat(context.getBeansOfType(com.kinplatform.common.knowledge.KnowledgeSource.class))
                    .isEmpty();
        });
    }

    @Test
    void conExternalEnabled_debeRegistrarElAdapterHttpSeguro() {
        runner.withPropertyValues(
                        "kin.knowledge.external-enabled=true",
                        "kin.knowledge.allowed-domains=autorizado.com",
                        "kin.knowledge.http.base-url=https://autorizado.com/search")
                .run(context -> {
                    assertThat(context).hasBean("externalHttpKnowledgeSource");
                    assertThat(context.getBean("externalHttpKnowledgeSource"))
                            .isInstanceOf(HttpKnowledgeSourceAdapter.class);
                });
    }

    @Test
    void conTestSourceEnabled_debeRegistrarLaFuenteControlada() {
        runner.withPropertyValues(
                        "kin.knowledge.test-source.enabled=true",
                        "kin.knowledge.test-source.candidates[0].content=dato controlado",
                        "kin.knowledge.test-source.candidates[0].url=https://data.autorizado.com/facto")
                .run(context -> {
                    assertThat(context).hasBean("controlledTestKnowledgeSource");
                    assertThat(context.getBean("controlledTestKnowledgeSource"))
                            .isInstanceOf(ControlledTestKnowledgeSource.class);
                });
    }

    @Test
    void sinEnable_noDebeRegistrarElAdapterHttp() {
        runner.withPropertyValues("kin.knowledge.external-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("externalHttpKnowledgeSource"));
    }

    @Test
    void conFuentesConfiguradas_debeRegistrarElCompositeMultiFuente() {
        runner.withPropertyValues(
                        "kin.knowledge.external-enabled=true",
                        "kin.knowledge.allowed-domains=api.worldbank.org,www.datos.gov.co",
                        "kin.knowledge.sources[0].id=worldbank-pib",
                        "kin.knowledge.sources[0].name=PIB",
                        "kin.knowledge.sources[0].base-url=https://api.worldbank.org/v2/country/COL/indicator/NY.GDP.MKTP.CD?format=json",
                        "kin.knowledge.sources[0].format=WORLD_BANK_V2",
                        "kin.knowledge.sources[0].query-param=",
                        "kin.knowledge.sources[1].id=trm-col",
                        "kin.knowledge.sources[1].name=TRM",
                        "kin.knowledge.sources[1].base-url=https://www.datos.gov.co/resource/32sa-8pi3.json?$limit=5",
                        "kin.knowledge.sources[1].format=SODA_JSON",
                        "kin.knowledge.sources[1].query-param=",
                        "kin.knowledge.sources[1].fact-columns[0]=valor")
                .run(context -> {
                    assertThat(context).hasBean("externalCompositeKnowledgeSource");
                    assertThat(context.getBean("externalCompositeKnowledgeSource"))
                            .isInstanceOf(CategoryAwareCompositeKnowledgeSource.class);
                });
    }

    @Test
    void sinExternalEnabled_noDebeRegistrarElComposite() {
        runner.withPropertyValues(
                        "kin.knowledge.external-enabled=false",
                        "kin.knowledge.sources[0].id=s1",
                        "kin.knowledge.sources[0].base-url=https://a.example/x")
                .run(context -> assertThat(context).doesNotHaveBean("externalCompositeKnowledgeSource"));
    }

    @Test
    void decoderJson_deberiaConstruirItemsDesdeElContrato() {
        var decoder =
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(new com.fasterxml.jackson.databind.ObjectMapper());
        var response = new HttpKnowledgeSourceAdapter.HttpResponse(
                200,
                "application/json",
                "{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\",\"publishedAt\":\"2026-01-15T10:00:00-05:00\"}]}");

        var items = decoder.apply(response);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).content()).isEqualTo("dato");
        assertThat(items.get(0).url()).isEqualTo("https://a.com/1");
        assertThat(items.get(0).publishedAt()).isNotNull();
    }

    @Test
    void decoderJson_cuerpoInvalido_deberiaDevolverListaVacia() {
        var decoder =
                KnowledgeHttpAutoConfiguration.jsonItemsDecoder(new com.fasterxml.jackson.databind.ObjectMapper());

        assertThat(decoder.apply(new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "no-json")))
                .isEmpty();
        assertThat(decoder.apply(new HttpKnowledgeSourceAdapter.HttpResponse(500, "application/json", "")))
                .isEmpty();
        assertThat(decoder.apply(null)).isEmpty();
    }
}

