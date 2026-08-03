package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Decorador del repositorio del proyecto empresarial que publica progreso
 * (Fase 10, Milestone 2J).
 *
 * <p>Envuelve un {@link EnterpriseProjectRepository} y, en cada
 * {@link #save}, publica la secuencia de eventos de progreso correspondiente
 * al estado del aggregate (via {@link EnterpriseProgressPublisher}) después de
 * persistirlo. Las consultas de lectura se delegan sin cambios. Así el flujo de
 * generación existente (contrato congelado) emite progreso observable vía SSE
 * sin modificar la máquina de estados ni los motores: el decorador se cablea en
 * la composición (Open/Closed). Clase stateless y thread-safe.</p>
 */
public final class ProgressPublishingEnterpriseProjectRepository
        implements EnterpriseProjectRepository {

    private final EnterpriseProjectRepository delegate;
    private final EnterpriseProgressPublisher publisher;

    /**
     * @param delegate  repositorio real al que se delega la persistencia
     * @param publisher publicador de progreso (obligatorio)
     */
    public ProgressPublishingEnterpriseProjectRepository(EnterpriseProjectRepository delegate,
                                                         EnterpriseProgressPublisher publisher) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate no puede ser null");
        }
        if (publisher == null) {
            throw new IllegalArgumentException("publisher no puede ser null");
        }
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public EnterpriseProject save(EnterpriseProject project) {
        EnterpriseProject saved = delegate.save(project);
        publisher.publishFor(project);
        return saved;
    }

    @Override
    public Optional<EnterpriseProject> findLatestVersion(UUID projectId) {
        return delegate.findLatestVersion(projectId);
    }

    @Override
    public Optional<EnterpriseProject> findByVersion(UUID projectId, int version) {
        return delegate.findByVersion(projectId, version);
    }

    @Override
    public List<EnterpriseProject> findAllVersions(UUID projectId) {
        return delegate.findAllVersions(projectId);
    }
}
