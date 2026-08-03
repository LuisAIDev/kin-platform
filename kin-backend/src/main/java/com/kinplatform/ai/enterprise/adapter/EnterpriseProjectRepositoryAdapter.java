package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JPA del puerto {@link EnterpriseProjectRepository} (Fase 10,
 * Milestone 2G).
 *
 * <p>Persiste el aggregate {@link EnterpriseProject} en las tablas
 * {@code enterprise_project} y {@code enterprise_document} mediante
 * {@link EnterpriseProjectJpaRepository} y {@link EnterpriseProjectMapper}. El
 * versionado es persistente por la clave natural {@code (projectId, version)}:
 * {@link #save} usa la fusión de JPA (alta o actualización de la misma versión,
 * con documentos en cascada y borrado de huérfanos) y las consultas recuperan
 * la última versión, una versión concreta o todas las versiones en orden.</p>
 *
 * <p>Adaptador de infraestructura: sin lógica de negocio, sin acoplamiento al
 * dominio (los tipos de dominio solo se usan en las firmas del puerto).</p>
 */
@Component
public class EnterpriseProjectRepositoryAdapter implements EnterpriseProjectRepository {

    private final EnterpriseProjectJpaRepository repository;
    private final EnterpriseProjectMapper mapper;

    /**
     * @param repository repositorio Spring Data (obligatorio)
     */
    public EnterpriseProjectRepositoryAdapter(EnterpriseProjectJpaRepository repository) {
        this(repository, new EnterpriseProjectMapper());
    }

    /**
     * @param repository repositorio Spring Data (obligatorio)
     * @param mapper     mapeador entidad ⇄ dominio (obligatorio)
     */
    public EnterpriseProjectRepositoryAdapter(EnterpriseProjectJpaRepository repository,
                                              EnterpriseProjectMapper mapper) {
        if (repository == null) {
            throw new IllegalArgumentException("repository no puede ser null");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("mapper no puede ser null");
        }
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EnterpriseProject save(EnterpriseProject project) {
        if (project == null) {
            throw new IllegalArgumentException("project no puede ser null");
        }
        EnterpriseProjectEntity persisted = repository.saveAndFlush(mapper.toEntity(project));
        return mapper.toDomain(persisted);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnterpriseProject> findLatestVersion(UUID projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return repository.findTopByProjectIdOrderByVersionDesc(projectId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnterpriseProject> findByVersion(UUID projectId, int version) {
        if (projectId == null) {
            return Optional.empty();
        }
        return repository.findByProjectIdAndVersion(projectId, version).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnterpriseProject> findAllVersions(UUID projectId) {
        if (projectId == null) {
            return List.of();
        }
        return repository.findByProjectIdOrderByVersionAsc(projectId).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
