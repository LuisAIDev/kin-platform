package com.kinplatform.kin.context;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del {@link ProjectContext}.
 *
 * <p>Invierte la dependencia del runtime respecto del estado en memoria: el
 * pipeline (vía {@code KinMethod}) depende de esta abstracción, no de una
 * implementación concreta. La durabilidad es responsabilidad del adaptador
 * (actualmente JPA/H2; puede reemplazarse sin tocar el dominio).</p>
 */
public interface ContextRepository {

    /**
     * Devuelve el contexto del proyecto o lo crea sembrado desde los datos
     * básicos del proyecto cuando todavía no existe.
     */
    ProjectContext findOrCreate(UUID projectId, String projectTitle,
                                String projectDescription, String projectCategory);

    Optional<ProjectContext> find(UUID projectId);

    void save(UUID projectId, ProjectContext context);

    void delete(UUID projectId);
}
