package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Repositorio en memoria del proyecto empresarial para pruebas (Fase 10,
 * Milestone 2E): guarda las versiones por {@code (projectId, version)} y
 * devuelve la más reciente en la consulta de última versión. Público para
 * compartirse con los tests de integración del flujo de conversación
 * (Milestone 2F).
 */
public final class InMemoryEnterpriseProjectRepository implements EnterpriseProjectRepository {

    private final Map<UUID, TreeMap<Integer, EnterpriseProject>> store = new HashMap<>();

    @Override
    public EnterpriseProject save(EnterpriseProject project) {
        store.computeIfAbsent(project.projectId(), k -> new TreeMap<>())
            .put(project.version(), project);
        return project;
    }

    @Override
    public Optional<EnterpriseProject> findLatestVersion(UUID projectId) {
        var versions = store.get(projectId);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versions.lastEntry().getValue());
    }

    @Override
    public Optional<EnterpriseProject> findByVersion(UUID projectId, int version) {
        var versions = store.get(projectId);
        if (versions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(versions.get(version));
    }

    @Override
    public List<EnterpriseProject> findAllVersions(UUID projectId) {
        var versions = store.get(projectId);
        if (versions == null) {
            return List.of();
        }
        return new ArrayList<>(versions.values());
    }
}
