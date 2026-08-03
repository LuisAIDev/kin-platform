package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.engine.BusinessModelEngine;
import com.kinplatform.kin.enterprise.engine.DefaultBusinessModelEngine;
import com.kinplatform.kin.enterprise.engine.DefaultEnterpriseScoreEngine;
import com.kinplatform.kin.enterprise.engine.DefaultFinancialPlanEngine;
import com.kinplatform.kin.enterprise.engine.DefaultInnovationEngine;
import com.kinplatform.kin.enterprise.engine.DefaultKpiEngine;
import com.kinplatform.kin.enterprise.engine.DefaultMarketEngine;
import com.kinplatform.kin.enterprise.engine.DefaultRiskPlanEngine;
import com.kinplatform.kin.enterprise.engine.DefaultRoadmapEngine;
import com.kinplatform.kin.enterprise.engine.EnterpriseScoreEngine;
import com.kinplatform.kin.enterprise.engine.FinancialPlanEngine;
import com.kinplatform.kin.enterprise.engine.InnovationEngine;
import com.kinplatform.kin.enterprise.engine.KpiEngine;
import com.kinplatform.kin.enterprise.engine.MarketEngine;
import com.kinplatform.kin.enterprise.engine.RiskPlanEngine;
import com.kinplatform.kin.enterprise.engine.RoadmapEngine;
import com.kinplatform.kin.enterprise.engine.input.BusinessModelInput;
import com.kinplatform.kin.enterprise.engine.input.EnterpriseScoreInput;
import com.kinplatform.kin.enterprise.engine.input.FinancialPlanInput;
import com.kinplatform.kin.enterprise.engine.input.InnovationInput;
import com.kinplatform.kin.enterprise.engine.input.KpiInput;
import com.kinplatform.kin.enterprise.engine.input.MarketInput;
import com.kinplatform.kin.enterprise.engine.input.RiskPlanInput;
import com.kinplatform.kin.enterprise.engine.input.RoadmapInput;
import com.kinplatform.kin.enterprise.engine.result.BusinessModelResult;
import com.kinplatform.kin.enterprise.engine.result.EnterpriseScoreResult;
import com.kinplatform.kin.enterprise.engine.result.FinancialPlanResult;
import com.kinplatform.kin.enterprise.engine.result.InnovationResult;
import com.kinplatform.kin.enterprise.engine.result.KpiResult;
import com.kinplatform.kin.enterprise.engine.result.MarketResult;
import com.kinplatform.kin.enterprise.engine.result.RiskPlanResult;
import com.kinplatform.kin.enterprise.engine.result.RoadmapResult;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectFailed;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectGenerated;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskResult;

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
public final class EnterpriseGenerationService {

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

    /**
     * Constructor principal con los ocho motores, el ensamblador de
     * documentos, los puertos de salida y el ejecutor para la generación
     * asíncrona.
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
        this(new DefaultBusinessModelEngine(), new DefaultMarketEngine(),
            new DefaultInnovationEngine(), new DefaultFinancialPlanEngine(),
            new DefaultRoadmapEngine(), new DefaultRiskPlanEngine(),
            new DefaultKpiEngine(), new DefaultEnterpriseScoreEngine(),
            documentAssembler, repository, eventBus, ForkJoinPool.commonPool());
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

        EnterpriseProject running = persistedRequested.startGeneration();
        repository.save(running);

        try {
            List<DocumentArtifact> documents = generateDocuments(request, running.version());
            EnterpriseProject withDocuments = running;
            for (DocumentArtifact document : documents) {
                withDocuments = withDocuments.attachDocument(document);
            }
            EnterpriseProject completed = withDocuments.completeGeneration();
            EnterpriseProject saved = repository.save(completed);
            eventBus.publish(new EnterpriseProjectGenerated(projectId, saved.version()));
            return saved;
        } catch (RuntimeException ex) {
            return fail(projectId, running, ex);
        }
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
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de generación no puede ser null.");
        }
        return CompletableFuture.supplyAsync(() -> generate(request), executor);
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
     * Ejecuta los ocho motores en orden de dependencia y ensambla los
     * documentos de la versión. El Enterprise Score se calcula como parte de la
     * coordinación (depende de todos los planes) aunque no se documente en el
     * aggregate.
     */
    private List<DocumentArtifact> generateDocuments(EnterpriseGenerationRequest request, int version) {
        ProjectContext context = request.context();
        RecommendationResult recommendations = request.recommendations();
        OpportunityResult opportunities = request.opportunities();
        KnowledgeResult knowledge = request.knowledge();
        RiskResult riskResult = request.riskResult();

        BusinessModelResult businessModel = nonNull(businessModelEngine.evaluate(
            new BusinessModelInput(context, recommendations, opportunities, knowledge)),
            BusinessModelResult.empty());
        MarketResult market = nonNull(marketEngine.evaluate(
            new MarketInput(context, recommendations, opportunities, knowledge)),
            MarketResult.empty());
        InnovationResult innovation = nonNull(innovationEngine.evaluate(
            new InnovationInput(context, opportunities, knowledge)),
            InnovationResult.empty());
        FinancialPlanResult financialPlan = nonNull(financialPlanEngine.evaluate(
            new FinancialPlanInput(context, market.plan(), recommendations)),
            FinancialPlanResult.empty());
        RoadmapResult roadmap = nonNull(roadmapEngine.evaluate(
            new RoadmapInput(context, recommendations, financialPlan.plan())),
            RoadmapResult.empty());
        RiskPlanResult riskPlan = nonNull(riskPlanEngine.evaluate(
            new RiskPlanInput(riskResult, financialPlan.plan())),
            RiskPlanResult.empty());
        KpiResult kpi = nonNull(kpiEngine.evaluate(
            new KpiInput(context, market.plan(), financialPlan.plan())),
            KpiResult.empty());
        nonNull(enterpriseScoreEngine.evaluate(new EnterpriseScoreInput(
            context, businessModel.canvas(), market.plan(), innovation.plan(),
            financialPlan.plan(), riskPlan.matrix(), roadmap.roadmap(), kpi.kpis(),
            recommendations, opportunities, knowledge, riskResult)),
            EnterpriseScoreResult.empty());

        return documentAssembler.assemble(version, businessModel, market, innovation,
            financialPlan, roadmap, riskPlan, kpi);
    }

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
