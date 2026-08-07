package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.event.DomainEventBus;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

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
 */
public final class EnterpriseProjectRequestedListener {

    private final EnterpriseGenerationOrchestrator orchestrator;
    private final ContextRepository contextRepository;
    private final Executor executor;
    private final EnterprisePipelineResultStore pipelineResultStore;

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
        this(orchestrator, contextRepository, eventBus, executor, NO_OP_RESULT_STORE);
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
        this.orchestrator = requireNonNull(orchestrator, "orchestrator");
        this.contextRepository = requireNonNull(contextRepository, "contextRepository");
        this.executor = requireNonNull(executor, "executor");
        this.pipelineResultStore = requireNonNull(pipelineResultStore, "pipelineResultStore");
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
        executor.execute(() -> {
            try {
                Optional<ProjectContext> context = contextRepository.find(event.projectId());
                if (context.isEmpty()) {
                    return;
                }
                EnterpriseTurnResults turnResults =
                        pipelineResultStore.consume(event.projectId()).orElse(EnterpriseTurnResults.empty());
                EnterpriseGenerationRequest request = new EnterpriseGenerationRequest(
                        event.projectId(), context.get(),
                        turnResults.recommendations(), turnResults.opportunities(),
                        turnResults.knowledge(), turnResults.riskResult());
                orchestrator.generateRequested(request, event.version());
            } catch (RuntimeException ex) {
                // La generación registra su propio EnterpriseProjectFailed; no se propaga.
            }
        });
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        return value;
    }
}
