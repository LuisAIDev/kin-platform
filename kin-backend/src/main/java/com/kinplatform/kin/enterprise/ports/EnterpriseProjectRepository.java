package com.kinplatform.kin.enterprise.ports;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del proyecto empresarial (Fase 10).
 *
 * <p>Contrato hexagonal de salida: permite al dominio persistir y recuperar el
 * aggregate {@link EnterpriseProject} — versionado por la clave natural
 * {@code (projectId, version)} — sin acoplarse a ninguna tecnología de
 * almacenamiento. La implementación (p. ej. {@code JpaEnterpriseProjectRepository})
 * es un adaptador que se aportará en el Milestone 2.</p>
 */
public interface EnterpriseProjectRepository {

    /**
     * Persiste el proyecto empresarial (alta o actualización de una versión).
     *
     * <p>Devuelve el aggregate persistido (en lugar de {@code void}) para que
     * el orquestador disponga del estado autoritativo tras la escritura sin una
     * segunda consulta: la versión asignada y las colecciones ya normalizadas.
     * Así los eventos de dominio ({@code EnterpriseProjectGenerated}/
     * {@code EnterpriseProjectFailed}) se emiten con la versión exacta
     * persistida, evitando lecturas adicionales y carreras entre generaciones
     * concurrentes de un mismo proyecto.</p>
     *
     * @param project proyecto empresarial a persistir
     * @return el proyecto empresarial persistido
     */
    EnterpriseProject save(EnterpriseProject project);

    /**
     * Recupera la versión más reciente del proyecto empresarial.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return la última versión, o vacío si no existe ninguna
     */
    Optional<EnterpriseProject> findLatestVersion(UUID projectId);

    /**
     * Recupera una versión concreta del proyecto empresarial.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión solicitada
     * @return la versión solicitada, o vacío si no existe
     */
    Optional<EnterpriseProject> findByVersion(UUID projectId, int version);

    /**
     * Recupera todas las versiones del proyecto empresarial, ordenadas por
     * versión ascendente.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return lista (posiblemente vacía) de las versiones del proyecto
     */
    List<EnterpriseProject> findAllVersions(UUID projectId);
}
