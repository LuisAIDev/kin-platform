package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Fachada de exportación de documentos del proyecto empresarial (Fase 10,
 * Milestone 2H).
 *
 * <p>Caso de uso de aplicación que coordina la exportación completa de una
 * versión: recupera el aggregate {@link EnterpriseProject} del
 * {@link EnterpriseProjectRepository} (puerto de persistencia) y delega la
 * generación de representaciones binarias en {@link EnterpriseExportService}.
 * Es el punto de entrada público del flujo de exportación que consumirán los
 * adaptadores REST de milestones posteriores.</p>
 *
 * <p>El constructor por defecto cablea la {@link EnterpriseRendererFactory}
 * con los renderizadores del módulo (PDF, DOCX y PPTX); el constructor tipado
 * permite inyectar un servicio de exportación propio. Clase stateless y
 * thread-safe.</p>
 */
public final class EnterpriseExportOrchestrator {

    private final EnterpriseProjectRepository repository;
    private final EnterpriseExportService exportService;

    /**
     * Fachada con la configuración por defecto (renderizadores del módulo).
     *
     * @param repository repositorio del proyecto empresarial (obligatorio)
     */
    public EnterpriseExportOrchestrator(EnterpriseProjectRepository repository) {
        this(repository, new EnterpriseExportService(new EnterpriseRendererFactory()));
    }

    /**
     * @param repository    repositorio del proyecto empresarial (obligatorio)
     * @param exportService servicio de exportación (obligatorio)
     */
    public EnterpriseExportOrchestrator(EnterpriseProjectRepository repository,
                                        EnterpriseExportService exportService) {
        if (repository == null) {
            throw new IllegalArgumentException("repository no puede ser null");
        }
        if (exportService == null) {
            throw new IllegalArgumentException("exportService no puede ser null");
        }
        this.repository = repository;
        this.exportService = exportService;
    }

    /**
     * Exporta una versión concreta del proyecto empresarial en todos los
     * formatos soportados.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión a exportar
     * @return bundle inmutable con las representaciones binarias
     * @throws EnterpriseExportException si la versión no existe
     * @throws IllegalArgumentException  si {@code projectId} es {@code null}
     */
    public EnterpriseDocumentBundle export(UUID projectId, int version) {
        EnterpriseProject project = repository.findByVersion(projectId, version)
            .orElseThrow(() -> new EnterpriseExportException(
                "No existe la versión " + version + " del proyecto " + projectId + "."));
        return exportService.export(project);
    }

    /**
     * Exporta la versión más reciente del proyecto empresarial en todos los
     * formatos soportados.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @return bundle inmutable con las representaciones binarias
     * @throws EnterpriseExportException si el proyecto no tiene versiones
     * @throws IllegalArgumentException  si {@code projectId} es {@code null}
     */
    public EnterpriseDocumentBundle exportLatest(UUID projectId) {
        EnterpriseProject project = repository.findLatestVersion(projectId)
            .orElseThrow(() -> new EnterpriseExportException(
                "El proyecto " + projectId + " no tiene versiones exportables."));
        return exportService.export(project);
    }

    /**
     * Exporta una versión si existe, o devuelve vacío en caso contrario.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión a exportar
     * @return bundle si la versión existe, vacío en caso contrario
     */
    public Optional<EnterpriseDocumentBundle> exportIfPresent(UUID projectId, int version) {
        return repository.findByVersion(projectId, version)
            .map(exportService::export);
    }
}
