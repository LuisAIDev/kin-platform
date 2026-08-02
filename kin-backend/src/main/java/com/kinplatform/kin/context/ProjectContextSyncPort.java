package com.kinplatform.kin.context;

import java.util.UUID;

/**
 * Puerto de sincronización del {@link ProjectContext} hacia el agregado
 * {@code Project} (capa de aplicación).
 *
 * <p>Resuelve la desincronización entre el estado analítico (persistido en
 * {@code project_context}) y los campos de presentación del {@code Project}
 * (tabla {@code projects}) que consume el Dashboard. Definido en el dominio
 * (inversión de dependencia): {@code KinMethod} lo invoca tras persistir el
 * contexto; la implementación vive en la capa de aplicación y decide, sin
 * sobrescribir información válida, qué campos reflejar.</p>
 */
public interface ProjectContextSyncPort {

    /**
     * Sincroniza los campos de presentación del {@code Project} a partir del
     * contexto analítico del proyecto.
     *
     * @param projectId identificador del proyecto (agregado {@code Project})
     * @param context   contexto analítico producido por el pipeline
     */
    void sync(UUID projectId, ProjectContext context);
}
