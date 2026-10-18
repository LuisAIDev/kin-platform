package com.kinplatform.platform.enterprise.application;

import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.platform.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.platform.enterprise.integration.EnterpriseDataProvider;
import com.kinplatform.platform.enterprise.integration.EnterpriseIntegrationData;
import com.kinplatform.platform.enterprise.integration.EnterpriseSupplementalInput;
import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.eventbus.IdempotencyService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener de aplicación del evento {@code EnterpriseProjectRequested}
 * (Fase 10, Milestone 2F).
 *
 * <p>Captura el evento publicado por el pipeline (vía
 * {@link EnterpriseProjectTrigger}) y delega la generación en
 * {@link EnterpriseGenerationOrchestrator} de forma totalmente asíncrona: el
 * turno de conversación termina exactamente igual que hoy y la generación del
 * proyecto empresarial comienza después, nunca antes ni durante.</p>
 *
 * <p>La solicitud de generación se construye a partir del contexto durable del
 * proyecto ({@link ContextRepository#find}), la única fuente de datos
 * disponible en la capa de aplicación sin infraestructura REST: los resultados
 * del pipeline viajan en el evento con la versión solicitada y el contexto se
 * recupera del repositorio. Cuando el contexto no existe (caso defensivo) la
 * generación se omite sin efectos.</p>
 *
 * <p>El listener se suscribe al {@link DomainEventBus} en su constructor: la
 * construcción equivale al cableado (composition root). La generación nunca
 * lanza fuera del ejecutor: cualquier fallo queda registrado por el propio
 * {@code EnterpriseGenerationService} como {@code EnterpriseProjectFailed}.</p>
 *
 * <p>Control de presupuesto de IA (Fase 1): si se inyecta un
 * {@link EnterpriseAiBudgetGate}, la generación pasa por la misma autoridad de
 * presupuesto de Fase 1 antes de invocar al LLM; sin presupuesto suficiente la
 * generación se omite (no se llama a DeepSeek).</p>
 *
 * <p>Idempotencia (PR 4): el listener verifica {@link IdempotencyService}
 * antes de procesar para evitar duplicados por reintentos del outbox relé
 * (semántica at-least-once). El registro en {@code processed_events} ocurre
 * dentro de la misma transacción que el procesamiento.</p>
 */
public final class EnterpriseProjectRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseProjectRequestedListener.class);

    private final EnterpriseGenerationOrchestrator orchestrator;
    private final ContextRepository contextRepository;
    private final Executor executor;
    private final EnterprisePipelineResultStore pipelineResultStore;
    private final EnterpriseAiBudgetGate aiBudgetGate;
    private final IdempotencyService idempotencyService;
    private final EnterpriseDataProvider dataProvider;

    /** Store no operativo (sin resultados del pipeline → offline-first). */
    private static final EnterprisePipelineResultStore NO_OP_RESULT_STORE = new EnterprisePipelineResultStore() {
        @Override
        public void store(EnterpriseTurnResults results) {}

        @Override
        public Optional<EnterpriseTurnResults> consume(UUID projectId) {
            return Optional.empty();
        }
    };

    /**
     * Construye el listener y lo suscribe al bus de eventos.
     *
     * @param orchestrator     orquestador que ejecuta la generación (obligatorio)
     * @param contextRepository repositorio del contexto durable (obligatorio)
     * @param eventBus         bus de eventos de dominio existente (obligatorio)
     * @param executor         ejecutor para la generación asíncrona (obligatorio)
     */
    public EnterpriseProjectRequestedListener(
            EnterpriseGenerationOrchestrator orchestrator,
            ContextRepository contextRepository,
            DomainEventBus eventBus,
            Executor executor) {
        this(orchestrator, contextRepository, eventBus, executor, NO_OP_RESULT_STORE, null, null);
    }

    /**
     * Constructor aditivo (Fase 10, Milestone 3C): inyecta la
     * {@link EnterprisePipelineResultStore} con los resultados reales del
     * pipeline del turno {@code REPORT}, que se fusionan en la
     * {@code EnterpriseGenerationRequest}. Sin store, la generación opera en
     * modo offline-first (resultados vacíos), comportamiento previo.
     *
     * @param orchestrator       orquestador que ejecuta la generación (obligatorio)
     * @param contextRepository  repositorio del contexto durable (obligatorio)
     * @param eventBus           bus de eventos de dominio existente (obligatorio)
     * @param executor           ejecutor para la generación asíncrona (obligatorio)
     * @param pipelineResultStore resultados reales del pipeline (obligatorio)
     */
    public EnterpriseProjectRequestedListener(
            EnterpriseGenerationOrchestrator orchestrator,
            ContextRepository contextRepository,
            DomainEventBus eventBus,
            Executor executor,
            EnterprisePipelineResultStore pipelineResultStore) {
        this(orchestrator, contextRepository, eventBus, executor, pipelineResultStore, null, null);
    }

    /**
     * Constructor con gate de presupuesto de IA (Fase 1): la generación
     * Enterprise (que ejecuta llamadas al LLM) pasa por la misma autoridad de
     * presupuesto; sin presupuesto NO se invoca a DeepSeek. {@code null} =
     * sin gate (comportamiento previo).
     */
    public EnterpriseProjectRequestedListener(
            EnterpriseGenerationOrchestrator orchestrator,
            ContextRepository contextRepository,
            DomainEventBus eventBus,
            Executor executor,
            EnterprisePipelineResultStore pipelineResultStore,
            EnterpriseAiBudgetGate aiBudgetGate) {
        this(orchestrator, contextRepository, eventBus, executor, pipelineResultStore, aiBudgetGate, null);
    }

    /**
     * Constructor con idempotencia (PR 4): inyecta el servicio de idempotencia
     * para evitar procesar eventos duplicados del outbox relé.
     *
     * @param orchestrator        orquestador que ejecuta la generación (obligatorio)
     * @param contextRepository   repositorio del contexto durable (obligatorio)
     * @param eventBus            bus de eventos de dominio existente (obligatorio)
     * @param executor            ejecutor para la generación asíncrona (obligatorio)
     * @param pipelineResultStore resultados reales del pipeline (obligatorio)
     * @param aiBudgetGate        gate de presupuesto de IA (opcional, {@code null} = sin gate)
     * @param idempotencyService  servicio de idempotencia (opcional, {@code null} = sin idempotencia)
     */
    public EnterpriseProjectRequestedListener(
            EnterpriseGenerationOrchestrator orchestrator,
            ContextRepository contextRepository,
            DomainEventBus eventBus,
            Executor executor,
            EnterprisePipelineResultStore pipelineResultStore,
            EnterpriseAiBudgetGate aiBudgetGate,
            IdempotencyService idempotencyService) {
        this(orchestrator, contextRepository, eventBus, executor, pipelineResultStore,
                aiBudgetGate, idempotencyService, null);
    }

    /**
     * Constructor con el proveedor de datos de integración (Fase 8.5b): el
     * listener reconstruye el supplemental numérico desde
     * {@code project_structured_info} antes de generar, de modo que el motor de
     * mercado reciba TAM/SAM/SOM y no quede en cero.
     *
     * @param orchestrator        orquestador que ejecuta la generación (obligatorio)
     * @param contextRepository   repositorio del contexto durable (obligatorio)
     * @param eventBus            bus de eventos de dominio existente (obligatorio)
     * @param executor            ejecutor para la generación asíncrona (obligatorio)
     * @param pipelineResultStore resultados reales del pipeline (obligatorio)
     * @param aiBudgetGate        gate de presupuesto de IA (opcional)
     * @param idempotencyService  servicio de idempotencia (opcional)
     * @param dataProvider        proveedor de datos de integración (opcional)
     */
    public EnterpriseProjectRequestedListener(
            EnterpriseGenerationOrchestrator orchestrator,
            ContextRepository contextRepository,
            DomainEventBus eventBus,
            Executor executor,
            EnterprisePipelineResultStore pipelineResultStore,
            EnterpriseAiBudgetGate aiBudgetGate,
            IdempotencyService idempotencyService,
            EnterpriseDataProvider dataProvider) {
        this.orchestrator = requireNonNull(orchestrator, "orchestrator");
        this.contextRepository = requireNonNull(contextRepository, "contextRepository");
        this.executor = requireNonNull(executor, "executor");
        this.pipelineResultStore = requireNonNull(pipelineResultStore, "pipelineResultStore");
        this.aiBudgetGate = aiBudgetGate;
        this.idempotencyService = idempotencyService;
        this.dataProvider = dataProvider;
        requireNonNull(eventBus, "eventBus").subscribe(EnterpriseProjectRequested.class, this::onRequested);
    }

    /**
     * Maneja el evento de solicitud: delega la generación de la versión
     * solicitada de forma asíncrona en el ejecutor, fusionando los resultados
     * reales del pipeline del turno cuando existen.
     *
     * @param event evento de solicitud recibido
     */
    public void onRequested(EnterpriseProjectRequested event) {
        if (event == null) {
            return;
        }
        // Idempotencia: usar correlationId del evento o generar uno basado en projectId + version
        String eventId = event.correlationId() != null ? event.correlationId() :
                "enterprise-requested-" + event.projectId() + "-" + event.version();
        if (idempotencyService != null && !idempotencyService.tryMarkProcessed(
                eventId, event.projectId(), event.getClass().getName())) {
            log.debug("Evento duplicado ignorado (idempotencia): eventId={}", eventId);
            return;
        }
        executor.execute(() -> {
            try {
                boolean proceed = aiBudgetGate == null || aiBudgetGate.reserve(event.projectId());
                if (!proceed) {
                    log.warn(
                            "Generación Enterprise omitida: presupuesto de IA insuficiente (projectId={})",
                            event.projectId());
                    return;
                }
                Optional<ProjectContext> context = contextRepository.find(event.projectId());
                if (context.isEmpty()) {
                    return;
                }
                EnterpriseTurnResults turnResults =
                        pipelineResultStore.consume(event.projectId()).orElse(EnterpriseTurnResults.empty());
                EnterpriseGenerationRequest request = new EnterpriseGenerationRequest(
                        event.projectId(),
                        context.get(),
                        turnResults.recommendations(),
                        turnResults.opportunities(),
                        turnResults.knowledge(),
                        turnResults.riskResult());
                if (dataProvider == null) {
                    log.warn(
                            "EnterpriseDataProvider no configurado; generación sin supplemental numérico (project={}).",
                            event.projectId());
                    orchestrator.generateRequested(request, event.version());
                    return;
                }
                EnterpriseSupplementalInput supplemental = loadSupplemental(event.projectId(), context.get());
                orchestrator.generateRequestedWithSupplemental(request, event.version(), supplemental);
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo en la generación asíncrona Enterprise: project={} version={}.",
                        event.projectId(), event.version(), ex);
            } finally {
                if (aiBudgetGate != null) {
                    aiBudgetGate.clear();
                }
            }
        });
    }

    private EnterpriseSupplementalInput loadSupplemental(UUID projectId, ProjectContext context) {
        try {
            EnterpriseIntegrationData data = dataProvider.load(projectId, context);
            if (data == null || data.supplementalData() == null) {
                log.warn(
                        "EnterpriseDataProvider devolvió datos nulos; se genera sin supplemental (project={}).",
                        projectId);
                return EnterpriseSupplementalInput.empty();
            }
            EnterpriseSupplementalInput supplemental =
                    EnterpriseSupplementalInput.from(data.supplementalData());
            if (supplemental.isEmpty()) {
                log.warn(
                        "Supplemental numérico vacío; el mercado puede quedar sin datos (project={}).",
                        projectId);
            }
            return supplemental;
        } catch (RuntimeException ex) {
            log.warn(
                    "No se pudo reconstruir el supplemental; se genera sin datos numéricos (project={}).",
                    projectId, ex);
            return EnterpriseSupplementalInput.empty();
        }
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        return value;
    }
}




