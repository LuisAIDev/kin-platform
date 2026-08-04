package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectAccessControl;
import com.kinplatform.kin.enterprise.application.DefaultEnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.application.EnterpriseExportOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseExportService;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationService;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectRequestedListener;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.application.EnterpriseRendererFactory;
import com.kinplatform.kin.enterprise.application.InMemoryEnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test del cableado Spring de la capa de aplicación del módulo Enterprise
 * (Fase 10, Milestone 2I): verifica que {@link EnterpriseWebConfig} compone los
 * orquestadores de generación y exportación y el mapeador con los puertos de
 * infraestructura disponibles, sin modificar los contratos congelados.
 */
class EnterpriseWebConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(EnterpriseWebConfig.class, SupportBeans.class);

    @Test
    void config_deberiaCablearLosBeansDeAplicacion() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(EnterpriseRendererFactory.class);
            assertThat(context).hasSingleBean(EnterpriseExportService.class);
            assertThat(context).hasSingleBean(EnterpriseExportOrchestrator.class);
            assertThat(context).hasSingleBean(EnterpriseGenerationService.class);
            assertThat(context).hasSingleBean(EnterpriseGenerationOrchestrator.class);
            assertThat(context).hasSingleBean(EnterpriseWebMapper.class);
            assertThat(context).hasSingleBean(Executor.class);
            assertThat(context).hasSingleBean(EnterpriseProjectTrigger.class);
            assertThat(context).hasSingleBean(DefaultEnterpriseProjectTrigger.class);
            assertThat(context).hasSingleBean(EnterpriseProjectRequestedListener.class);
        });
    }

    @Test
    void config_deberiaExponerLosOrquestadoresFuncionales() {
        runner.run(context -> {
            var export = context.getBean(EnterpriseExportOrchestrator.class);
            var generation = context.getBean(EnterpriseGenerationOrchestrator.class);
            var mapper = context.getBean(EnterpriseWebMapper.class);
            assertThat(export).isNotNull();
            assertThat(generation).isNotNull();
            assertThat(mapper).isNotNull();
        });
    }

    /**
     * Beans de soporte de infraestructura para el contexto de prueba.
     */
    @Configuration
    static class SupportBeans {

        @Bean
        EnterpriseProjectRepository enterpriseProjectRepository() {
            return new InMemoryEnterpriseProjectRepository();
        }

        @Bean
        DomainEventBus domainEventBus() {
            return new InMemoryDomainEventBus();
        }

        @Bean
        ContextRepository contextRepository() {
            return mock(ContextRepository.class);
        }

        @Bean
        AIResponder aiResponder() {
            return mock(AIResponder.class);
        }

        @Bean
        EnterpriseProjectAccessControl enterpriseProjectAccessControl() {
            return mock(EnterpriseProjectAccessControl.class);
        }
    }
}
