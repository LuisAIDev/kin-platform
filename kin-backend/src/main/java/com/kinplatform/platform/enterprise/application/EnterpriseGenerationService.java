package com.kinplatform.platform.enterprise.application;

import com.kinplatform.common.ai.AIResponder;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.platform.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.platform.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.platform.enterprise.engine.BusinessModelEngine;
import com.kinplatform.platform.enterprise.engine.DefaultBusinessModelEngine;
import com.kinplatform.platform.enterprise.engine.DefaultEnterpriseScoreEngine;
import com.kinplatform.platform.enterprise.engine.DefaultFinancialPlanEngine;
import com.kinplatform.platform.enterprise.engine.DefaultInnovationEngine;
import com.kinplatform.platform.enterprise.engine.DefaultKpiEngine;
import com.kinplatform.platform.enterprise.engine.DefaultMarketEngine;
import com.kinplatform.platform.enterprise.engine.DefaultRiskPlanEngine;
import com.kinplatform.platform.enterprise.engine.DefaultRoadmapEngine;
import com.kinplatform.platform.enterprise.engine.EnterpriseScoreEngine;
import com.kinplatform.platform.enterprise.engine.FinancialPlanEngine;
import com.kinplatform.platform.enterprise.engine.InnovationEngine;
import com.kinplatform.platform.enterprise.engine.KpiEngine;
import com.kinplatform.platform.enterprise.engine.MarketEngine;
import com.kinplatform.platform.enterprise.engine.RiskPlanEngine;
import com.kinplatform.platform.enterprise.engine.RoadmapEngine;
import com.kinplatform.platform.enterprise.engine.input.BusinessModelInput;
import com.kinplatform.platform.enterprise.engine.input.EnterpriseScoreInput;
import com.kinplatform.platform.enterprise.engine.input.FinancialPlanInput;
import com.kinplatform.platform.enterprise.engine.input.InnovationInput;
import com.kinplatform.platform.enterprise.engine.input.KpiInput;
import com.kinplatform.platform.enterprise.engine.input.MarketInput;
import com.kinplatform.platform.enterprise.engine.input.RiskPlanInput;
import com.kinplatform.platform.enterprise.engine.input.RoadmapInput;
import com.kinplatform.platform.enterprise.engine.result.BusinessModelResult;
import com.kinplatform.platform.enterprise.engine.result.EnterpriseScoreResult;
import com.kinplatform.platform.enterprise.engine.result.FinancialPlanResult;
import com.kinplatform.platform.enterprise.engine.result.InnovationResult;
import com.kinplatform.platform.enterprise.engine.result.KpiResult;
import com.kinplatform.platform.enterprise.engine.result.MarketResult;
import com.kinplatform.platform.enterprise.engine.result.RiskPlanResult;
import com.kinplatform.platform.enterprise.engine.result.RoadmapResult;
import com.kinplatform.platform.enterprise.events.EnterpriseProjectFailed;
import com.kinplatform.platform.enterprise.events.EnterpriseProjectGenerated;
import com.kinplatform.platform.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.platform.enterprise.integration.EnterpriseSupplementalInput;
import com.kinplatform.platform.enterprise.integration.NumericPlanIntegrator;
import com.kinplatform.platform.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.platform.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.platform.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.platform.enterprise.valueobjects.MarketPlan;
import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import com.kinplatform.common.knowledge.KnowledgeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.kinplatform.platform.reporting.RecommendationResult;
import com.kinplatform.platform.reporting.opportunity.OpportunityResult;
import com.kinplatform.platform.reporting.risk.RiskResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Servicio de aplicación de generación del proyecto empresarial (Fase 10,
 * Milestone 2E).
 *
 * <p>Coordina el flujo completo de generación del {@link EnterpriseProject}:
 * determinación de versión, máquina de estados
 * ({@code REQUESTED → RUNNING → COMPLETED | FAILED}), ejecución ordenada de
 * los ocho motores deterministas, ensamblado de documentos, persistencia y
 * emisión de los eventos de dominio ({@code EnterpriseProjectRequested},
 * {@code EnterpriseProjectGenerated}, {@code EnterpriseProjectFailed}).</p>
 *
 * <h2>Flujo de generación</h2>
 * <ol>
 *   <li><b>Idempotencia</b>: si la última versión del proyecto está en vuelo
 *       ({@code REQUESTED} o {@code RUNNING}) se devuelve tal cual, sin
 *       reiniciar la generación ni emitir eventos duplicados.</li>
 *   <li><b>Versión</b>: sin versiones previas se crea la {@code 1}; desde un
 *       estado terminal ({@code COMPLETED}/{@code FAILED}) se crea la
 *       siguiente versión.</li>
 *   <li><b>Solicitud</b>: se persiste el aggregate {@code REQUESTED} y se
 *       emite {@code EnterpriseProjectRequested}.</li>
 *   <li><b>Ejecución</b>: se persiste {@code RUNNING} y se ejecutan los ocho
 *       motores en orden de dependencia
 *       (modelo de negocio &rarr; mercado &rarr; innovaci&oacute;n &rarr;
 *       plan financiero &rarr; hoja de ruta &rarr; riesgos &rarr; KPIs
 *       &rarr; Enterprise Score).</li>
 *   <li><b>Documentos</b>: los resultados no vacíos se ensamblan en
 *       {@link DocumentArtifact} y se adjuntan al aggregate.</li>
 *   <li><b>Cierre</b>: se persiste {@code COMPLETED} y se emite
 *       {@code EnterpriseProjectGenerated}; ante cualquier
 *       {@link RuntimeException} se persiste {@code FAILED} y se emite
 *       {@code EnterpriseProjectFailed}.</li>
 * </ol>
 *
 * <p>El Enterprise Score se calcula (octavo motor) pero no se persiste en este
 * milestone: el aggregate solo porta documentos y la puntuación se consumirá
 * en el dashboard (Milestone posterior). El renderizado a PDF/Word y la
 * integración REST/SSE quedan fuera de este milestone.</p>
 *
 * <p>Servicio de dominio puro, sin Spring: no se registra en el contenedor ni
 * en {@code EngineRegistry} (decisión de aislamiento del paquete
 * {@code engine}). La capa de aplicación compone los motores y los puertos
 * directamente.</p>
 */
public class EnterpriseGenerationService {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseGenerationService.class);

    private final BusinessModelEngine businessModelEngine;
    private final MarketEngine marketEngine;
    private final InnovationEngine innovationEngine;
    private final FinancialPlanEngine financialPlanEngine;
    private final RoadmapEngine roadmapEngine;
    private final RiskPlanEngine riskPlanEngine;
    private final KpiEngine kpiEngine;
    private final EnterpriseScoreEngine enterpriseScoreEngine;
    private final EnterpriseDocumentAssembler documentAssembler;
    private final EnterpriseProjectRepository repository;
    private final DomainEventBus eventBus;
    private final Executor executor;
    private final EnterpriseNarrativeGenerator narrativeGenerator;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TransactionTemplate transactionTemplate;

    /**
     * Constructor principal con los ocho motores, el ensamblador de
     * documentos, los puertos de salida y el ejecutor para la generación
     * asíncrona. Sin generador narrativo (IA), la generación produce solo los
     * documentos deterministas.
     */
    public EnterpriseGenerationService(
            BusinessModelEngine businessModelEngine,
            MarketEngine marketEngine,
            InnovationEngine innovationEngine,
            FinancialPlanEngine financialPlanEngine,
            RoadmapEngine roadmapEngine,
            RiskPlanEngine riskPlanEngine,
            KpiEngine kpiEngine,
            EnterpriseScoreEngine enterpriseScoreEngine,
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            Executor executor) {
        this(
                businessModelEngine,
                marketEngine,
                innovationEngine,
                financialPlanEngine,
                roadmapEngine,
                riskPlanEngine,
                kpiEngine,
                enterpriseScoreEngine,
                documentAssembler,
                repository,
                eventBus,
                executor,
                null);
    }

    /**
     * Constructor aditivo (Fase 10, Milestone 3E): inyecta el puerto de IA
     * {@code AIResponder} que genera los documentos narrativos (Executive
     * Report y DOFA) al final de la generación, sin sustituir los motores
     * deterministas.
     */
    public EnterpriseGenerationService(
            BusinessModelEngine businessModelEngine,
            MarketEngine marketEngine,
            InnovationEngine innovationEngine,
            FinancialPlanEngine financialPlanEngine,
            RoadmapEngine roadmapEngine,
            RiskPlanEngine riskPlanEngine,
            KpiEngine kpiEngine,
            EnterpriseScoreEngine enterpriseScoreEngine,
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            Executor executor,
            AIResponder aiResponder) {
        this(
                businessModelEngine,
                marketEngine,
                innovationEngine,
                financialPlanEngine,
                roadmapEngine,
                riskPlanEngine,
                kpiEngine,
                enterpriseScoreEngine,
                documentAssembler,
                repository,
                eventBus,
                executor,
                aiResponder,
                null,
                null);
    }

    /**
     * Constructor aditivo (Fase 8.1): inyecta el publicador del outbox para
     * encolar de forma transaccional la solicitud de generación asíncrona. Sin
     * publicador se conserva el comportamiento previo (ejecución en el
     * ejecutor interno).
     */
    public EnterpriseGenerationService(
            BusinessModelEngine businessModelEngine,
            MarketEngine marketEngine,
            InnovationEngine innovationEngine,
            FinancialPlanEngine financialPlanEngine,
            RoadmapEngine roadmapEngine,
            RiskPlanEngine riskPlanEngine,
            KpiEngine kpiEngine,
            EnterpriseScoreEngine enterpriseScoreEngine,
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            Executor executor,
            AIResponder aiResponder,
            OutboxEventPublisher outboxEventPublisher,
            PlatformTransactionManager transactionManager) {
        this.businessModelEngine = requireNonNull(businessModelEngine, "businessModelEngine");
        this.marketEngine = requireNonNull(marketEngine, "marketEngine");
        this.innovationEngine = requireNonNull(innovationEngine, "innovationEngine");
        this.financialPlanEngine = requireNonNull(financialPlanEngine, "financialPlanEngine");
        this.roadmapEngine = requireNonNull(roadmapEngine, "roadmapEngine");
        this.riskPlanEngine = requireNonNull(riskPlanEngine, "riskPlanEngine");
        this.kpiEngine = requireNonNull(kpiEngine, "kpiEngine");
        this.enterpriseScoreEngine = requireNonNull(enterpriseScoreEngine, "enterpriseScoreEngine");
        this.documentAssembler = requireNonNull(documentAssembler, "documentAssembler");
        this.repository = requireNonNull(repository, "repository");
        this.eventBus = requireNonNull(eventBus, "eventBus");
        this.executor = requireNonNull(executor, "executor");
        this.narrativeGenerator = aiResponder == null
                ? null
                : new EnterpriseNarrativeGenerator(
                        aiResponder, documentAssembler, new EnterpriseNarrativePromptBuilder(documentAssembler));
        this.outboxEventPublisher = outboxEventPublisher;
        this.transactionTemplate = transactionManager == null
                ? null
                : new TransactionTemplate(transactionManager);
    }

    /**
     * Constructor de conveniencia con los motores deterministas por defecto y
     * el ejecutor compartido del {@code ForkJoinPool}: no es necesario pasar
     * implementaciones concretas para orquestar el flujo de dominio.
     */
    public EnterpriseGenerationService(
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus) {
        this(documentAssembler, repository, eventBus, null);
    }

    /**
     * Constructor de conveniencia aditivo (Fase 10, Milestone 3E): motores
     * deterministas por defecto más el puerto de IA para la generación
     * narrativa.
     */
    public EnterpriseGenerationService(
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            AIResponder aiResponder) {
        this(
                new DefaultBusinessModelEngine(),
                new DefaultMarketEngine(),
                new DefaultInnovationEngine(),
                new DefaultFinancialPlanEngine(),
                new DefaultRoadmapEngine(),
                new DefaultRiskPlanEngine(),
                new DefaultKpiEngine(),
                new DefaultEnterpriseScoreEngine(),
                documentAssembler,
                repository,
                eventBus,
                ForkJoinPool.commonPool(),
                aiResponder);
    }

    /**
     * Constructor de conveniencia aditivo (Fase 8.1): motores deterministas por
     * defecto, IA narrativa y publicador del outbox para la solicitud asíncrona.
     */
    public EnterpriseGenerationService(
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            AIResponder aiResponder,
            OutboxEventPublisher outboxEventPublisher) {
        this(
                new DefaultBusinessModelEngine(),
                new DefaultMarketEngine(),
                new DefaultInnovationEngine(),
                new DefaultFinancialPlanEngine(),
                new DefaultRoadmapEngine(),
                new DefaultRiskPlanEngine(),
                new DefaultKpiEngine(),
                new DefaultEnterpriseScoreEngine(),
                documentAssembler,
                repository,
                eventBus,
                ForkJoinPool.commonPool(),
                aiResponder,
                outboxEventPublisher,
                null);
    }

    /**
     * Constructor de conveniencia aditivo (Fase 8.2): motores deterministas por
     * defecto, IA narrativa, publicador del outbox y el gestor de transacciones
     * con el que se garantiza el commit del evento aunque el bean no sea
     * proxificado por Spring.
     */
    public EnterpriseGenerationService(
            EnterpriseDocumentAssembler documentAssembler,
            EnterpriseProjectRepository repository,
            DomainEventBus eventBus,
            AIResponder aiResponder,
            OutboxEventPublisher outboxEventPublisher,
            PlatformTransactionManager transactionManager) {
        this(
                new DefaultBusinessModelEngine(),
                new DefaultMarketEngine(),
                new DefaultInnovationEngine(),
                new DefaultFinancialPlanEngine(),
                new DefaultRoadmapEngine(),
                new DefaultRiskPlanEngine(),
                new DefaultKpiEngine(),
                new DefaultEnterpriseScoreEngine(),
                documentAssembler,
                repository,
                eventBus,
                ForkJoinPool.commonPool(),
                aiResponder,
                outboxEventPublisher,
                transactionManager);
    }

    /**
     * Genera el proyecto empresarial de forma bloqueante.
     *
     * @param request solicitud de generación (obligatoria)
     * @return el aggregate persistido (terminal o en vuelo si ya había una
     *         generación activa — idempotencia)
     * @throws IllegalArgumentException si {@code request} es {@code null}
     */
    public EnterpriseProject generate(EnterpriseGenerationRequest request) {
        return generateInternal(request, EnterpriseSupplementalInput.empty());
    }

    /**
     * Genera el proyecto empresarial de forma bloqueante incorporando los datos
     * numéricos estructurados del usuario (FASE 10B). Con entrada vacía se
     * comporta exactamente igual que {@link #generate(EnterpriseGenerationRequest)}.
     *
     * @param request      solicitud de generación (obligatoria)
     * @param supplemental datos numéricos estructurados (puede ser vacío)
     * @return el aggregate persistido
     */
    public EnterpriseProject generateWithSupplemental(
            EnterpriseGenerationRequest request, EnterpriseSupplementalInput supplemental) {
        return generateInternal(request, supplemental);
    }

    private EnterpriseProject generateInternal(
            EnterpriseGenerationRequest request, EnterpriseSupplementalInput supplemental) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de generación no puede ser null.");
        }
        UUID projectId = request.projectId();

        Optional<EnterpriseProject> latest = repository.findLatestVersion(projectId);

        EnterpriseProject requested = requestedVersion(projectId, latest);
        if (requested == null) {
            return latest.orElseThrow();
        }

        EnterpriseProject persistedRequested = repository.save(requested);
        eventBus.publish(new EnterpriseProjectRequested(projectId, persistedRequested.version()));

        return generateFrom(request, persistedRequested, supplemental);
    }

    /**
     * Genera una versión concreta del proyecto empresarial sin publicar
     * {@code EnterpriseProjectRequested} (Fase 10, Milestone 2F).
     *
     * <p>Es el punto de entrada del flujo integrado con el pipeline: el
     * {@link EnterpriseProjectTrigger} ya publicó el evento de solicitud con la
     * versión resuelta y el {@code EnterpriseProjectRequestedListener} delega
     * aquí de forma asíncrona. El evento no se publica de nuevo (una sola
     * solicitud por generación). Idempotente: si la versión ya existe
     * (en vuelo o terminal) se devuelve tal cual.</p>
     *
     * @param request solicitud de generación (obligatoria)
     * @param version versión solicitada (mayor o igual a 1)
     * @return el aggregate persistido de la versión solicitada
     * @throws IllegalArgumentException si {@code request} es {@code null} o la
     *                                  versión es inválida
     */
    public EnterpriseProject generateRequested(EnterpriseGenerationRequest request, int version) {
        return generateRequestedWithSupplemental(request, version, EnterpriseSupplementalInput.empty());
    }

    /**
     * Genera una versión concreta sin volver a publicar {@code EnterpriseProjectRequested},
     * incorporando los datos numéricos estructurados (FASE 10B).
     */
    public EnterpriseProject generateRequestedWithSupplemental(
            EnterpriseGenerationRequest request, int version, EnterpriseSupplementalInput supplemental) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de generación no puede ser null.");
        }
        if (version < 1) {
            throw new IllegalArgumentException(
                    "La versión solicitada debe ser mayor o igual a 1 (recibida: " + version + ").");
        }
        UUID projectId = request.projectId();
        Optional<EnterpriseProject> existing = repository.findByVersion(projectId, version);
        if (existing.isPresent()) {
            EnterpriseProject current = existing.get();
            if (!current.isRequested()) {
                log.info(
                        "Generación idempotente: project={} version={} ya está en estado {}; no se reinicia.",
                        projectId, version, current.status());
                return current;
            }
            // REQUESTED huérfano (solicitud encolada que nunca arrancó): se
            // reanuda la generación en lugar de devolverlo sin hacer nada.
            log.info("Reanudando generación REQUESTED: project={} version={}.", projectId, version);
            return generateFrom(request, current, supplemental);
        }
        EnterpriseProject requested = EnterpriseProject.request(projectId, version);
        log.info("Creando versión REQUESTED: project={} version={}.", projectId, version);
        return generateFrom(request, repository.save(requested), supplemental);
    }

    /**
     * Genera el proyecto empresarial de forma asíncrona en el ejecutor del
     * servicio. Las excepciones del flujo quedan reflejadas en el aggregate
     * {@code FAILED} persistido y en {@code EnterpriseProjectFailed}, no en el
     * futuro.
     *
     * @param request solicitud de generación (obligatoria)
     * @return futuro que completa con el aggregate persistido
     * @throws IllegalArgumentException si {@code request} es {@code null}
     */
    public CompletableFuture<EnterpriseProject> generateAsync(EnterpriseGenerationRequest request) {
        return generateAsyncWithSupplemental(request, EnterpriseSupplementalInput.empty());
    }

    /**
     * Genera el proyecto empresarial de forma asíncrona incorporando los datos
     * numéricos estructurados (FASE 10B).
     */
    public CompletableFuture<EnterpriseProject> generateAsyncWithSupplemental(
            EnterpriseGenerationRequest request, EnterpriseSupplementalInput supplemental) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de generación no puede ser null.");
        }
        if (outboxEventPublisher == null || transactionTemplate == null) {
            log.warn(
                    "Outbox no disponible (publisher={}, transactionTemplate={}); ejecución inline no durable para project={}.",
                    outboxEventPublisher != null, transactionTemplate != null, request.projectId());
            return CompletableFuture.supplyAsync(() -> generateWithSupplemental(request, supplemental), executor);
        }
        UUID projectId = request.projectId();
        Optional<EnterpriseProject> latest = repository.findLatestVersion(projectId);
        EnterpriseProject requested = requestedVersion(projectId, latest);
        // Si la última versión ya está REQUESTED/RUNNING, requestedVersion()
        // devuelve null. Se re-encola ESA MISMA versión para que el listener la
        // retome (caso típico: REQUESTED huérfano que nunca arrancó).
        EnterpriseProject baseline = requested != null ? requested : latest.orElse(null);
        if (baseline == null) {
            throw new IllegalStateException(
                    "No existe ninguna versión empresarial que encolar para project=" + projectId);
        }
        int versionToEnqueue = baseline.version();
        // correlationId único por solicitud: evita que la idempotencia del
        // listener descarte un reintento legítimo del usuario.
        String correlationId = UUID.randomUUID().toString();
        log.info(
                "Encolando EnterpriseProjectRequested: project={} version={} correlationId={} nuevaVersion={}.",
                projectId, versionToEnqueue, correlationId, requested != null);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                log.info(
                        "Transacción outbox abierta: project={} version={}. Llamando a publish...",
                        projectId, versionToEnqueue);
                outboxEventPublisher.publish(
                        new EnterpriseProjectRequested(projectId, versionToEnqueue, correlationId));
                log.info("publish() completado: project={} version={}.", projectId, versionToEnqueue);
            });
        } catch (RuntimeException ex) {
            log.error(
                    "ERROR CRÍTICO guardando EnterpriseProjectRequested en outbox: project={} version={}.",
                    projectId, versionToEnqueue, ex);
            throw ex;
        }
        log.info(
                "Transacción confirmada: evento persistido en domain_event_outbox (project={} version={}).",
                projectId, versionToEnqueue);
        return CompletableFuture.completedFuture(baseline);
    }

    // ------------------------------------------------------------------
    // Flujo interno
    // ------------------------------------------------------------------

    /**
     * Determina la versión {@code REQUESTED} a generar.
     *
     * <p>Devuelve {@code null} cuando la última versión ya está en vuelo
     * (idempotencia) y la generación debe devolverla sin reiniciar.</p>
     */
    private EnterpriseProject requestedVersion(UUID projectId, Optional<EnterpriseProject> latest) {
        if (latest.isEmpty()) {
            return EnterpriseProject.request(projectId, 1);
        }
        EnterpriseProject current = latest.get();
        if (current.isRequested() || current.isRunning()) {
            return null;
        }
        return current.nextVersion();
    }

    /**
     * Flujo común de generación a partir de un aggregate {@code REQUESTED}
     * persistido: transición a {@code RUNNING}, ejecución de los motores,
     * ensamblado de documentos, cierre {@code COMPLETED} y emisión de
     * {@code EnterpriseProjectGenerated}; ante cualquier
     * {@link RuntimeException} persiste {@code FAILED} y emite
     * {@code EnterpriseProjectFailed}.
     */
    private EnterpriseProject generateFrom(
            EnterpriseGenerationRequest request,
            EnterpriseProject requested,
            EnterpriseSupplementalInput supplemental) {
        UUID projectId = request.projectId();
        EnterpriseProject running = requested.startGeneration();
        repository.save(running);
        try {
            GeneratedContent content = generateContent(request, running.version(), supplemental);
            EnterpriseProject withDocuments = running;
            for (DocumentArtifact document : content.documents()) {
                withDocuments = withDocuments.attachDocument(document);
            }
            EnterpriseProject withScore =
                    content.score() != null ? withDocuments.withScore(content.score()) : withDocuments;
            EnterpriseProject withPartial = content.partial()
                    ? withScore.withPartial(true)
                    : withScore;
            EnterpriseProject completed = withPartial.completeGeneration();
            EnterpriseProject saved = repository.save(completed);
            eventBus.publish(new EnterpriseProjectGenerated(projectId, saved.version()));
            return saved;
        } catch (RuntimeException ex) {
            return fail(projectId, running, ex);
        }
    }

    /**
     * Ejecuta los ocho motores en orden de dependencia y produce los documentos
     * de la versión junto con el Enterprise Score calculado (Fase 10, M3D). El
     * score se calcula exclusivamente por el {@code EnterpriseScoreEngine}
     * (única fuente de verdad) y viaja con el contenido para adjuntarse al
     * aggregate.
     */
    private GeneratedContent generateContent(
            EnterpriseGenerationRequest request, int version, EnterpriseSupplementalInput supplemental) {
        ProjectContext context = request.context();
        RecommendationResult recommendations = request.recommendations();
        OpportunityResult opportunities = request.opportunities();
        KnowledgeResult knowledge = request.knowledge();
        RiskResult riskResult = request.riskResult();

        BusinessModelResult businessModel = nonNull(
                businessModelEngine.evaluate(
                        new BusinessModelInput(context, recommendations, opportunities, knowledge)),
                BusinessModelResult.empty());
        MarketResult market = nonNull(
                marketEngine.evaluate(new MarketInput(context, recommendations, opportunities, knowledge)),
                MarketResult.empty());
        if (market.isEmpty()) {
            log.warn(
                    "MarketResult vacío: project={} version={} supplementalEmpty={}; "
                            + "se intentará completar con datos estructurados.",
                    request.projectId(), version, supplemental == null || supplemental.isEmpty());
        }
        MarketPlan baseMarketPlan = market.plan() != null ? market.plan() : MarketPlan.empty();
        MarketPlan marketPlan = NumericPlanIntegrator.integrateMarketPlan(baseMarketPlan, supplemental);
        boolean partial = marketPlan.tam() <= 0.0 || marketPlan.som() <= 0.0;
        if (partial) {
            log.warn(
                    "Datos de mercado insuficientes: project={} version={} tam={} sam={} som={} "
                            + "supplementalEmpty={}; se marca isPartial=true.",
                    request.projectId(), version, marketPlan.tam(), marketPlan.sam(), marketPlan.som(),
                    supplemental == null || supplemental.isEmpty());
        }
        InnovationResult innovation = nonNull(
                innovationEngine.evaluate(new InnovationInput(context, opportunities, knowledge)),
                InnovationResult.empty());
        FinancialPlanResult financialPlan = nonNull(
                financialPlanEngine.evaluate(new FinancialPlanInput(context, marketPlan, recommendations)),
                FinancialPlanResult.empty());
        RoadmapResult roadmap = nonNull(
                roadmapEngine.evaluate(new RoadmapInput(context, recommendations, financialPlan.plan())),
                RoadmapResult.empty());
        RiskPlanResult riskPlan = nonNull(
                riskPlanEngine.evaluate(new RiskPlanInput(riskResult, financialPlan.plan())), RiskPlanResult.empty());
        KpiResult kpi =
                nonNull(kpiEngine.evaluate(new KpiInput(context, marketPlan, financialPlan.plan())), KpiResult.empty());
        EnterpriseScoreResult scoreResult = nonNull(
                enterpriseScoreEngine.evaluate(new EnterpriseScoreInput(
                        context,
                        businessModel.canvas(),
                        marketPlan,
                        innovation.plan(),
                        financialPlan.plan(),
                        riskPlan.matrix(),
                        roadmap.roadmap(),
                        kpi.kpis(),
                        recommendations,
                        opportunities,
                        knowledge,
                        riskResult)),
                EnterpriseScoreResult.empty());

        List<DocumentArtifact> documents = documentAssembler.assemble(
                version, businessModel, market, innovation, financialPlan, roadmap, riskPlan, kpi);
        if (narrativeGenerator != null && scoreResult.score() != null) {
            documents = withNarrativeDocuments(
                    version,
                    context,
                    recommendations,
                    opportunities,
                    knowledge,
                    riskResult,
                    scoreResult.score(),
                    documents);
        }
        return new GeneratedContent(documents, scoreResult.score(), partial);
    }

    /**
     * Anexa los documentos narrativos (Executive Report y DOFA) generados por
     * la IA al final de la generación, sin modificar los documentos
     * deterministas (Fase 10, Milestone 3E).
     */
    private List<DocumentArtifact> withNarrativeDocuments(
            int version,
            ProjectContext context,
            RecommendationResult recommendations,
            OpportunityResult opportunities,
            KnowledgeResult knowledge,
            RiskResult riskResult,
            EnterpriseScore score,
            List<DocumentArtifact> documents) {
        var all = new ArrayList<>(documents);
        all.addAll(narrativeGenerator.generate(
                version, context, recommendations, opportunities, knowledge, riskResult, score, documents));
        return List.copyOf(all);
    }

    /**
     * Contenido de la generación: documentos de la versión y Enterprise Score
     * calculado por el motor (puede ser {@code null} si el motor no produjo
     * puntuación).
     */
    private record GeneratedContent(List<DocumentArtifact> documents, EnterpriseScore score, boolean partial) {}

    /**
     * Persiste el estado {@code FAILED} y emite el evento de fallo.
     */
    private EnterpriseProject fail(UUID projectId, EnterpriseProject running, RuntimeException cause) {
        String reason = cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
        EnterpriseProject failed = running.failGeneration(reason);
        EnterpriseProject saved = repository.save(failed);
        eventBus.publish(new EnterpriseProjectFailed(projectId, saved.version(), reason));
        return saved;
    }

    private static <T> T nonNull(T value, T empty) {
        return value == null ? empty : value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        return value;
    }
}







