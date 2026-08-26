package com.kinplatform.kin.health.differential.config;

import com.kinplatform.kin.health.differential.adapter.DifferentialCatalogParser;
import com.kinplatform.kin.health.differential.adapter.DifferentialKnowledgeAdapter;
import com.kinplatform.kin.health.differential.engine.DifferentialEngine;
import com.kinplatform.kin.health.differential.port.DifferentialKnowledgeRepository;
import com.kinplatform.kin.health.differential.stage.DifferentialStage;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de diagnóstico diferencial (ADR-029).
 *
 * <p>Declara el motor, la etapa de pipeline, el adaptador de conocimiento, el
 * parser y el servicio de actualización del catálogo. La integración con el
 * pipeline principal se realiza en {@code KinConfig} (etapa aditiva
 * {@code DifferentialStage} justo después de {@code TriageStage}).</p>
 */
@Configuration
@EnableConfigurationProperties(DifferentialProperties.class)
public class DifferentialConfig {

    @Bean
    public DifferentialEngine differentialEngine(
            DifferentialKnowledgeRepository knowledgeRepository, DifferentialProperties properties) {
        return new DifferentialEngine(knowledgeRepository, properties.getMaxItems());
    }

    @Bean
    public DifferentialStage differentialStage(
            DifferentialEngine differentialEngine, DifferentialProperties properties) {
        return new DifferentialStage(differentialEngine, properties.isEnabled());
    }

    @Bean
    public DifferentialKnowledgeAdapter differentialKnowledgeAdapter(DifferentialProperties properties) {
        DifferentialProperties.Catalog catalog = properties.getCatalog();
        return new DifferentialKnowledgeAdapter(
                catalog.getSourceId(),
                catalog.getSourceName(),
                catalog.isExternalEnabled(),
                catalog.getBaseUrl(),
                catalog.getBundledResource());
    }

    @Bean
    public DifferentialCatalogParser differentialCatalogParser() {
        return new DifferentialCatalogParser();
    }

    @Bean
    public com.kinplatform.kin.health.differential.api.DifferentialCatalogUpdateService
            differentialCatalogUpdateService(
                    KnowledgeEngine knowledgeEngine,
                    DifferentialKnowledgeAdapter differentialKnowledgeAdapter,
                    DifferentialKnowledgeRepository knowledgeRepository,
                    TriageKnowledgeRepository triageKnowledgeRepository,
                    DifferentialCatalogParser differentialCatalogParser,
                    DifferentialProperties properties) {
        return new com.kinplatform.kin.health.differential.api.DifferentialCatalogUpdateService(
                knowledgeEngine,
                differentialKnowledgeAdapter,
                knowledgeRepository,
                triageKnowledgeRepository,
                differentialCatalogParser,
                properties);
    }
}
