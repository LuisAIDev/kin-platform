package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.event.DomainEventBus;

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
 */
public final class DefaultEnterpriseProjectTrigger implements EnterpriseProjectTrigger {

    private final EnterpriseProjectRepository repository;
    private final DomainEventBus eventBus;

    /**
     * @param repository repositorio del proyecto empresarial (obligatorio)
     * @param eventBus   bus de eventos de dominio existente (obligatorio)
     */
    public DefaultEnterpriseProjectTrigger(EnterpriseProjectRepository repository,
                                           DomainEventBus eventBus) {
        this.repository = requireNonNull(repository, "repository");
        this.eventBus = requireNonNull(eventBus, "eventBus");
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
        eventBus.publish(new EnterpriseProjectRequested(projectId, version));
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        return value;
    }
}
