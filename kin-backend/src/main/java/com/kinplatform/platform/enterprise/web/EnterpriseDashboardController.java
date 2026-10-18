package com.kinplatform.platform.enterprise.web;

import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.platform.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.platform.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.platform.enterprise.web.dto.EnterpriseDashboardResponse;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador REST del dashboard Enterprise (Fase 10, Milestone 2J).
 *
 * <p>Expone {@code GET /enterprise/{projectId}/{version}/dashboard}: vista
 * consolidada de una versión (estado, progreso, documentos, score, fechas,
 * versiones y estadísticas) para la pantalla Enterprise Dashboard. El
 * controlador únicamente delega en el puerto de persistencia y traduce el
 * resultado a DTO mediante {@link EnterpriseWebMapper}; no contiene lógica de
 * negocio.</p>
 */
@RestController
@RequestMapping({
    "/enterprise",
    "/empresas/enterprise"
})
@Tag(name = "Enterprise", description = "API del módulo Enterprise: dashboard "
    + "consolidado de una versión del proyecto empresarial.")
public class EnterpriseDashboardController {

    private final EnterpriseProjectRepository repository;
    private final EnterpriseWebMapper mapper;
    private final ContextRepository contextRepository;
    private final ProjectStructuredInfoRepository structuredInfoRepository;

    /**
     * Constructor de compatibilidad (pruebas): sin contexto disponible, el
     * dashboard conserva el progreso derivado del estado de generación.
     */
    public EnterpriseDashboardController(EnterpriseProjectRepository repository,
                                         EnterpriseWebMapper mapper) {
        this(repository, mapper, null, null);
    }

    /**
     * Constructor de compatibilidad con tests: sin repositorio de información
     * estructurada, {@code scoreStale} siempre es {@code false}.
     */
    public EnterpriseDashboardController(EnterpriseProjectRepository repository,
                                         EnterpriseWebMapper mapper,
                                         ContextRepository contextRepository) {
        this(repository, mapper, contextRepository, null);
    }

    /**
     * Constructor de producción: incorpora la completitud real de la
     * información del proyecto y detecta si el score es anterior a la última
     * actualización de {@code project_structured_info}.
     *
     * @param repository              puerto de persistencia del proyecto empresarial (obligatorio)
     * @param mapper                  mapeador dominio ? DTO (obligatorio)
     * @param contextRepository       contexto durable del proyecto (opcional)
     * @param structuredInfoRepository repositorio de información estructurada (opcional)
     */
    @Autowired
    public EnterpriseDashboardController(EnterpriseProjectRepository repository,
                                         EnterpriseWebMapper mapper,
                                         ContextRepository contextRepository,
                                         ProjectStructuredInfoRepository structuredInfoRepository) {
        this.repository = require(repository, "repository");
        this.mapper = require(mapper, "mapper");
        this.contextRepository = contextRepository;
        this.structuredInfoRepository = structuredInfoRepository;
    }

    @GetMapping("/{projectId}/{version}/dashboard")
    @Operation(summary = "Dashboard de una versión",
        description = "Devuelve la vista consolidada de una versión del proyecto "
            + "empresarial: estado, progreso, documentos, Enterprise Score, fechas, "
            + "versiones y estadísticas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard de la versión",
            content = @Content(schema = @Schema(implementation = EnterpriseDashboardResponse.class))),
        @ApiResponse(responseCode = "404", description = "La versión solicitada no existe",
            content = @Content(schema = @Schema(implementation = com.kinplatform.platform.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseDashboardResponse> getDashboard(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen",
            required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial",
            required = true, example = "1", in = ParameterIn.PATH)
        @PathVariable int version) {
        requireValidVersion(version);
        EnterpriseProject project = repository.findByVersion(projectId, version)
            .orElseThrow(() -> new EnterpriseNotFoundException(
                "No existe la versión " + version + " del proyecto " + projectId + "."));
        List<EnterpriseProject> versions = repository.findAllVersions(projectId);
        boolean scoreStale = isScoreStale(project, projectId);
        return ResponseEntity.ok(
            mapper.toDashboard(project, versions, informationProgress(projectId), scoreStale));
    }

    /**
     * Indica si el score persistido es anterior a la última actualización de la
     * información estructurada del proyecto.
     */
    private boolean isScoreStale(EnterpriseProject project, UUID projectId) {
        if (structuredInfoRepository == null || project.score() == null) {
            return false;
        }
        return structuredInfoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId).stream()
            .map(ProjectStructuredInfo::getUpdatedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .map(updatedAt -> updatedAt.isAfter(project.updatedAt()))
            .orElse(false);
    }

    /**
     * Completitud real de la información del proyecto (dimensiones cubiertas
     * sobre el total). Devuelve {@code null} si no hay contexto disponible para
     * conservar el progreso de generación como respaldo.
     */
    private Integer informationProgress(UUID projectId) {
        if (contextRepository == null) {
            return null;
        }
        return contextRepository.find(projectId)
            .map(context -> (int) Math.round(context.coverageRatio() * 100))
            .orElse(null);
    }

    private void requireValidVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException(
                "La versión debe ser mayor o igual a 1 (recibida: " + version + ").");
        }
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null");
        }
        return value;
    }
}



