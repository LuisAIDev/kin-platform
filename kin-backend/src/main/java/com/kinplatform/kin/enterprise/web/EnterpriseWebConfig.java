package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.application.EnterpriseExportOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseExportService;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationService;
import com.kinplatform.kin.enterprise.application.EnterpriseRendererFactory;
import com.kinplatform.kin.enterprise.application.ProgressPublishingEnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressPublisher;
import com.kinplatform.kin.event.DomainEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Cableado Spring de la capa de aplicación del módulo Enterprise (Fase 10,
 * Milestone 2I).
 *
 * <p>Define los beans de orquestación y exportación que consume el
 * {@link EnterpriseController} sin modificar los contratos congelados: las
 * clases de dominio siguen siendo POJOs y los motores enterprise NUNCA se
 * registran como beans de tipo {@code DomainEngine} (decisión de aislamiento
 * del {@code package-info} de {@code engine}). Esta configuración solo compone
 * los orquestadores y el mapeador web.</p>
 */
@Configuration
public class EnterpriseWebConfig {

    /**
     * Fábrica de renderizadores con los tres formatos por defecto (PDF, DOCX y PPTX).
     */
    @Bean
    public EnterpriseRendererFactory enterpriseRendererFactory() {
        return new EnterpriseRendererFactory();
    }

    /**
     * Servicio de exportación de documentos.
     */
    @Bean
    public EnterpriseExportService enterpriseExportService(EnterpriseRendererFactory rendererFactory) {
        return new EnterpriseExportService(rendererFactory);
    }

    /**
     * Fachada de exportación, recuperando la versión del repositorio.
     */
    @Bean
    public EnterpriseExportOrchestrator enterpriseExportOrchestrator(
            EnterpriseProjectRepository repository,
            EnterpriseExportService exportService) {
        return new EnterpriseExportOrchestrator(repository, exportService);
    }

    /**
     * Servicio de generación con los motores deterministas por defecto.
     */
    @Bean
    public EnterpriseGenerationService enterpriseGenerationService(
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus) {
        return new EnterpriseGenerationService(
            new EnterpriseDocumentAssembler(), repository, eventBus);
    }

    /**
     * Fachada de generación del proyecto empresarial.
     */
    @Bean
    public EnterpriseGenerationOrchestrator enterpriseGenerationOrchestrator(
            EnterpriseGenerationService generationService) {
        return new EnterpriseGenerationOrchestrator(generationService);
    }

    /**
     * Mapeador dominio ⇄ DTO de la API.
     */
    @Bean
    public EnterpriseWebMapper enterpriseWebMapper() {
        return new EnterpriseWebMapper();
    }

    /**
     * Servicio de progreso SSE (heartbeat de 15 s en segundo plano).
     */
    @Bean
    public EnterpriseProgressService enterpriseProgressService() {
        return new EnterpriseProgressService();
    }

    /**
     * Publicador de progreso (traduce el estado del aggregate a eventos SSE).
     */
    @Bean
    public EnterpriseProgressPublisher enterpriseProgressPublisher(
            EnterpriseProgressService progressService) {
        return new EnterpriseProgressPublisher(progressService);
    }

    /**
     * Repositorio decorado con publicación de progreso: envuelve al adaptador
     * JPA y publica el estado de cada {@code save} vía SSE. Se marca como
     * {@code @Primary} para que los orquestadores y el controlador usen el
     * decorador (las lecturas se delegan sin cambios).
     */
    @Bean
    @Primary
    public EnterpriseProjectRepository enterpriseProgressPublishingRepository(
            EnterpriseProjectRepository enterpriseProjectRepository,
            EnterpriseProgressPublisher enterpriseProgressPublisher) {
        return new ProgressPublishingEnterpriseProjectRepository(
            enterpriseProjectRepository, enterpriseProgressPublisher);
    }
}
