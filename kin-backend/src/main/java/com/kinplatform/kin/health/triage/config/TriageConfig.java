package com.kinplatform.kin.health.triage.config;

import com.kinplatform.kin.health.triage.adapter.CachedTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.adapter.HealthCatalogParser;
import com.kinplatform.kin.health.triage.adapter.HealthDataImporter;
import com.kinplatform.kin.health.triage.adapter.HealthKnowledgeAdapter;
import com.kinplatform.kin.health.triage.adapter.JpaTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.adapter.OpenNLPSymptomExtractorV2;
import com.kinplatform.kin.health.triage.domain.KeywordSymptomExtractor;
import com.kinplatform.kin.health.triage.domain.SymptomExtractor;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.stage.TriageStage;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wiring del módulo de triaje (ADR-028).
 *
 * <p>Declara los beans de dominio (motor y etapa de pipeline), el
 * {@link SymptomExtractor} (NLP con OpenNLP y fallback determinista por
 * keywords, o solo keywords si el NLP está deshabilitado) y los colaboradores de
 * infraestructura del enriquecimiento del catálogo (fase profesional):
 * {@link HealthKnowledgeAdapter}, {@link HealthCatalogParser} y
 * {@code TriageCatalogUpdateService}. La integración con el pipeline principal
 * se realiza en {@code KinConfig} (etapa aditiva {@code TriageStage}).</p>
 */
@Configuration
@EnableConfigurationProperties(TriageProperties.class)
public class TriageConfig {

    @Bean
    public TriageEngine triageEngine(TriageKnowledgeRepository knowledgeRepository, TriageProperties properties) {
        return new TriageEngine(knowledgeRepository, properties.getMaxConditions());
    }

    /**
     * Decorador de caché del catálogo (fase de producción): envuelve el
     * repositorio JPA con {@code @Cacheable}; {@code applyUpdate} invalida la
     * caché. Se marca {@code @Primary} para que todos los consumidores usen la
     * versión cacheada. Si el feature flag está deshabilitado, se expone el
     * repositorio JPA sin envolver.
     */
    @Bean
    @Primary
    public TriageKnowledgeRepository triageKnowledgeRepository(
            JpaTriageKnowledgeRepository jpaRepository, TriageProperties properties) {
        if (properties.isCatalogCacheEnabled()) {
            return new CachedTriageKnowledgeRepository(jpaRepository);
        }
        return jpaRepository;
    }

    @Bean
    public SymptomExtractor triageSymptomExtractor(TriageProperties properties) {
        if (properties.isNlpEnabled()) {
            return new OpenNLPSymptomExtractorV2();
        }
        return new KeywordSymptomExtractor();
    }

    @Bean
    public TriageStage triageStage(
            TriageEngine triageEngine,
            TriageKnowledgeRepository knowledgeRepository,
            SymptomExtractor symptomExtractor,
            TriageProperties properties) {
        return new TriageStage(triageEngine, knowledgeRepository, symptomExtractor, properties.isEnabled());
    }

    @Bean
    public HealthKnowledgeAdapter healthKnowledgeAdapter(TriageProperties properties) {
        TriageProperties.Catalog catalog = properties.getCatalog();
        return new HealthKnowledgeAdapter(
                catalog.getSourceId(),
                catalog.getSourceName(),
                catalog.isExternalEnabled(),
                catalog.getBaseUrl(),
                catalog.getBundledResource());
    }

    @Bean
    public HealthCatalogParser healthCatalogParser() {
        return new HealthCatalogParser();
    }

    @Bean
    public HealthDataImporter healthDataImporter(
            KnowledgeEngine knowledgeEngine,
            HealthKnowledgeAdapter healthKnowledgeAdapter,
            HealthCatalogParser healthCatalogParser,
            TriageKnowledgeRepository knowledgeRepository) {
        return new HealthDataImporter(
                knowledgeEngine, healthKnowledgeAdapter, healthCatalogParser, knowledgeRepository);
    }

    @Bean
    public com.kinplatform.kin.health.triage.api.TriageCatalogUpdateService triageCatalogUpdateService(
            KnowledgeEngine knowledgeEngine,
            HealthKnowledgeAdapter healthKnowledgeAdapter,
            TriageKnowledgeRepository knowledgeRepository,
            HealthCatalogParser healthCatalogParser,
            TriageProperties properties) {
        return new com.kinplatform.kin.health.triage.api.TriageCatalogUpdateService(
                knowledgeEngine, healthKnowledgeAdapter, knowledgeRepository, healthCatalogParser, properties);
    }
}

