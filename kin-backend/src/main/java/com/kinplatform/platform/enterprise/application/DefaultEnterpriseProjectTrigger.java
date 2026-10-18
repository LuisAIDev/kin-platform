package com.kinplatform.platform.enterprise.application;

import com.kinplatform.platform.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.platform.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.platform.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * Trigger por defecto de solicitud de generación del proyecto empresarial
 * (Fase 10, Milestone 2F).
 *
 * <p>Implementa {@link EnterpriseProjectTrigger}: resuelve la versión del
 * proyecto empresarial que corresponde solicitar y publica
 * {@link EnterpriseProjectRequested} en el {@link DomainEventBus} existente.
 * La versión se deriva del estado persistido del aggregate:</p>
 *
 * <ul>
 *   <li>Sin versiones previas → {@code 1}.</li>
 *   <li>Desde un estado terminal ({@code COMPLETED}/{@code FAILED}) → la
 *       siguiente versión.</li>
 *   <li>Con una generación en vuelo ({@code REQUESTED}/{@code RUNNING}) → no
 *       publica nada (idempotencia): la generación ya está en curso y el
 *       listener la completará.</li>
 * </ul>
 *
 * <p>La publicación es no bloqueante: el listener de
 * {@code EnterpriseProjectRequested} ejecuta la generación de forma asíncrona
 * y el turno de conversación no se ve afectado.</p>
 *
 * <p>Cuando se configura {@link OutboxEventPublisher} con un
 * {@link PlatformTransactionManager}, la solicitud se persiste en el outbox
 * transaccional en lugar de publicarse directamente en el bus en memoria. Así
 * el relay solo la entrega después del commit y no puede leer estado no
 * confirmado (carrera del chat). Sin esas dependencias se conserva la
 * publicación en el {@link DomainEventBus} para los escenarios existentes.</p>
 */
public final class DefaultEnterpriseProjectTrigger implements EnterpriseProjectTrigger {

    private final EnterpriseProjectRepository repository;
    private final DomainEventBus eventBus;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TransactionTemplate transactionTemplate;

    /**
     * Constructor de compatibilidad: publica directamente en el bus.
     *
     * @param repository repositorio del proyecto empresarial (obligatorio)
     * @param eventBus   bus de eventos de dominio existente (obligatorio)
     */
    public DefaultEnterpriseProjectTrigger(EnterpriseProjectRepository repository,
                                           DomainEventBus eventBus) {
        this(repository, eventBus, null, null);
    }

    /**
     * Constructor transaccional: persiste la solicitud en el outbox cuando se
     * proporcionan ambas dependencias; si se omiten ambas, delega en el bus.
     *
     * @param repository             repositorio del proyecto empresarial (obligatorio)
     * @param eventBus               bus de eventos de dominio existente (obligatorio)
     * @param outboxEventPublisher   publicador del outbox (opcional junto con el gestor)
     * @param transactionManager     gestor transaccional (opcional junto con el publicador)
     */
    public DefaultEnterpriseProjectTrigger(EnterpriseProjectRepository repository,
                                           DomainEventBus eventBus,
                                           OutboxEventPublisher outboxEventPublisher,
                                           PlatformTransactionManager transactionManager) {
        this.repository = requireNonNull(repository, "repository");
        this.eventBus = requireNonNull(eventBus, "eventBus");
        if ((outboxEventPublisher == null) != (transactionManager == null)) {
            throw new IllegalArgumentException(
                "outboxEventPublisher y transactionManager deben configurarse juntos o ambos omitirse.");
        }
        this.outboxEventPublisher = outboxEventPublisher;
        this.transactionTemplate = transactionManager == null
            ? null
            : new TransactionTemplate(transactionManager);
    }

    @Override
    public void request(UUID projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("El identificador del proyecto no puede ser null.");
        }
        Optional<EnterpriseProject> latest = repository.findLatestVersion(projectId);
        if (latest.isPresent()) {
            EnterpriseProject current = latest.get();
            if (current.isRequested() || current.isRunning()) {
                return;
            }
        }
        int version = latest.map(EnterpriseProject::nextVersion)
            .map(EnterpriseProject::version)
            .orElse(1);
        publish(new EnterpriseProjectRequested(projectId, version));
    }

    private void publish(EnterpriseProjectRequested event) {
        if (transactionTemplate != null && outboxEventPublisher != null) {
            transactionTemplate.executeWithoutResult(status -> outboxEventPublisher.publish(event));
            return;
        }
        eventBus.publish(event);
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        return value;
    }
}



