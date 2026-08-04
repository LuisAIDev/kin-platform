package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.enterprise.application.DefaultEnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.application.EnterpriseExportOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseExportService;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationService;
import com.kinplatform.kin.enterprise.application.EnterprisePipelineResultStore;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectRequestedListener;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.application.EnterpriseRendererFactory;
import com.kinplatform.kin.enterprise.application.InMemoryEnterprisePipelineResultStore;
import com.kinplatform.kin.enterprise.application.ProgressPublishingEnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectAccessControl;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressPublisher;
import com.kinplatform.kin.event.DomainEventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
     * Servicio de generación con los motores deterministas por defecto y la IA
     * narrativa (Fase 10, M3E): reutiliza el puerto {@link AIResponder} (bean
     * {@code AiEngineService}) que rutea DeepSeek/OpenAI/Ollama con fallback en
     * español. La IA solo redacta EXECUTIVE_REPORT y DOFA al final de la
     * generación; los motores deterministas siguen siendo la fuente de verdad.
     */
    @Bean
    public EnterpriseGenerationService enterpriseGenerationService(
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            AIResponder aiResponder) {
        return new EnterpriseGenerationService(
            new EnterpriseDocumentAssembler(), repository, eventBus, aiResponder);
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

    /**
     * Ejecutor dedicado para la generación asíncrona del proyecto empresarial
     * (Fase 10, M3B): pool acotado con threads daemon. La generación nunca
     * bloquea el turno de conversación; los fallos quedan registrados por el
     * propio servicio como {@code EnterpriseProjectFailed}.
     */
    @Bean
    public Executor enterpriseGenerationExecutor() {
        return Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "enterprise-generation");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Trigger del ciclo automático (Fase 10, M3B): puerto
     * {@link EnterpriseProjectTrigger} que consume la capa de conversación.
     * Resuelve la versión y publica {@code EnterpriseProjectRequested} en el
     * {@link DomainEventBus} cuando el pipeline completa {@code REPORT}.
     */
    @Bean
    public EnterpriseProjectTrigger enterpriseProjectTrigger(
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus) {
        return new DefaultEnterpriseProjectTrigger(repository, eventBus);
    }

    /**
     * Listener del ciclo automático (Fase 10, M3B): se suscribe al
     * {@link DomainEventBus} en su construcción, captura
     * {@code EnterpriseProjectRequested} y delega la generación en el
     * {@link EnterpriseGenerationOrchestrator} de forma asíncrona en el
     * ejecutor dedicado.
     */
    @Bean
    public EnterpriseProjectRequestedListener enterpriseProjectRequestedListener(
            EnterpriseGenerationOrchestrator enterpriseGenerationOrchestrator,
            ContextRepository contextRepository,
            DomainEventBus eventBus,
            Executor enterpriseGenerationExecutor,
            EnterprisePipelineResultStore enterprisePipelineResultStore) {
        return new EnterpriseProjectRequestedListener(
            enterpriseGenerationOrchestrator, contextRepository, eventBus,
            enterpriseGenerationExecutor, enterprisePipelineResultStore);
    }

    /**
     * Almacén de los resultados reales del pipeline (Fase 10, M3C): el runtime
     * publica los resultados del turno {@code REPORT} y el listener los
     * recupera para construir la {@code EnterpriseGenerationRequest} con datos
     * reales (TAM/SAM/SOM, riesgos, oportunidades, recomendaciones,
     * conocimiento). Implementación en memoria, correlación por
     * {@code projectId}, consumo único.
     */
    @Bean
    public EnterprisePipelineResultStore enterprisePipelineResultStore() {
        return new InMemoryEnterprisePipelineResultStore();
    }

    /**
     * Interceptor de autorización Enterprise (remediación C1, IDOR): verifica
     * que el {@code projectId} de cada ruta {@code /enterprise/**} pertenezca al
     * usuario autenticado; en caso contrario responde 404. Se registra
     * exclusivamente para el prefijo {@code /enterprise}.
     */
    @Bean
    public EnterpriseOwnershipInterceptor enterpriseOwnershipInterceptor(
            EnterpriseProjectAccessControl enterpriseProjectAccessControl) {
        return new EnterpriseOwnershipInterceptor(enterpriseProjectAccessControl);
    }

    /**
     * Registra el interceptor de autorización en el MVC para todas las rutas del
     * módulo Enterprise. El control de acceso se aplica a consulta, generación,
     * exportación y SSE.
     */
    @Bean
    public WebMvcConfigurer enterpriseMvcConfigurer(
            @Autowired EnterpriseOwnershipInterceptor enterpriseOwnershipInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(enterpriseOwnershipInterceptor)
                    .addPathPatterns("/enterprise/**");
            }
        };
    }
}
