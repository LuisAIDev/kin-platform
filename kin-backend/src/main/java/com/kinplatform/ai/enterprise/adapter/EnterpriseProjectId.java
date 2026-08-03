package com.kinplatform.ai.enterprise.adapter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave primaria compuesta del proyecto empresarial (Fase 10, Milestone 2G).
 *
 * <p>Modela la identidad natural del aggregate {@code EnterpriseProject}:
 * {@code (projectId, version)}. Es la clase {@code @IdClass} de
 * {@link EnterpriseProjectEntity} y define la igualdad que JPA/Hibernate
 * utiliza para la gestión de entidades versionadas.</p>
 *
 * <p>Clase de infraestructura: no forma parte del dominio y no contiene lógica
 * de negocio.</p>
 */
@Data
@NoArgsConstructor
public class EnterpriseProjectId implements Serializable {

    private UUID projectId;

    private int version;

    /**
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión del proyecto empresarial
     */
    public EnterpriseProjectId(UUID projectId, int version) {
        this.projectId = projectId;
        this.version = version;
    }
}
