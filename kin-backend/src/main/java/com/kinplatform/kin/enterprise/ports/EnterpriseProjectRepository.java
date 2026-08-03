package com.kinplatform.kin.enterprise.ports;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del proyecto empresarial (Fase 10).
 *
 * <p>Contrato hexagonal de salida: permite al dominio persistir y recuperar
 * el aggregate {@link EnterpriseProject} sin acoplarse a ninguna tecnología de
 * almacenamiento. La implementación (p. ej. {@code JpaEnterpriseProjectRepository})
 * es un adaptador que se aportará en el Milestone 2.</p>
 */
public interface EnterpriseProjectRepository {

    /**
     * Persiste el proyecto empresarial (alta o actualización).
     */
    void save(EnterpriseProject project);

    /**
     * Recupera el proyecto empresarial por identificador de proyecto.
     *
     * @return el aggregate, o vacío si no existe ninguna versión
     */
    Optional<EnterpriseProject> findByProjectId(UUID projectId);

    /**
     * Recupera la versión más reciente del proyecto empresarial.
     *
     * @return la última versión, o vacío si no existe
     */
    Optional<EnterpriseProject> findLatestVersion(UUID projectId);
}
