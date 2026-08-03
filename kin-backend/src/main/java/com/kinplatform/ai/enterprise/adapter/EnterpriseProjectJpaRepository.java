package com.kinplatform.ai.enterprise.adapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data del proyecto empresarial (Fase 10, Milestone 2G).
 *
 * <p>Acceso de infraestructura a la tabla {@code enterprise_project} mediante
 * consultas derivadas por la clave natural {@code (projectId, version)}: última
 * versión, versión concreta y todas las versiones ordenadas de forma
 * ascendente. Sin lógica de negocio.</p>
 */
public interface EnterpriseProjectJpaRepository
        extends JpaRepository<EnterpriseProjectEntity, EnterpriseProjectId> {

    /**
     * Devuelve la versión más reciente del proyecto, o vacío si no existe.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return la versión de mayor número, si existe
     */
    Optional<EnterpriseProjectEntity> findTopByProjectIdOrderByVersionDesc(UUID projectId);

    /**
     * Devuelve la versión concreta solicitada, o vacío si no existe.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión solicitada
     * @return la versión solicitada, si existe
     */
    Optional<EnterpriseProjectEntity> findByProjectIdAndVersion(UUID projectId, int version);

    /**
     * Devuelve todas las versiones del proyecto ordenadas por versión
     * ascendente.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return lista (posiblemente vacía) de las versiones del proyecto
     */
    List<EnterpriseProjectEntity> findByProjectIdOrderByVersionAsc(UUID projectId);
}
