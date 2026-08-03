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

    /**
     * Construye el listener y lo suscribe al bus de eventos.
     *
     * @param orchestrator     orquestador que ejecuta la generación (obligatorio)
     * @param contextRepository repositorio del contexto durable (obligatorio)
     * @param eventBus         bus de eventos de dominio existente (obligatorio)
     * @param executor         ejecutor para la generación asíncrona (obligatorio)
     */
    public EnterpriseProjectRequestedListener(EnterpriseGenerationOrchestrator orchestrator,
                                              ContextRepository contextRepository,
                                              DomainEventBus eventBus,
                                              Executor executor) {
        this.orchestrator = requireNonNull(orchestrator, "orchestrator");
        this.contextRepository = requireNonNull(contextRepository, "contextRepository");
        this.executor = requireNonNull(executor, "executor");
        requireNonNull(eventBus, "eventBus")
            .subscribe(EnterpriseProjectRequested.class, this::onRequested);
    }

    /**
     * Maneja el evento de solicitud: delega la generación de la versión
     * solicitada de forma asíncrona en el ejecutor.
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
                EnterpriseGenerationRequest request = new EnterpriseGenerationRequest(
                    event.projectId(), context.get(), null, null, null, null);
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
