package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
@RequestMapping("/enterprise")
@Tag(name = "Enterprise", description = "API del módulo Enterprise: dashboard "
    + "consolidado de una versión del proyecto empresarial.")
public class EnterpriseDashboardController {

    private final EnterpriseProjectRepository repository;
    private final EnterpriseWebMapper mapper;

    /**
     * @param repository puerto de persistencia del proyecto empresarial (obligatorio)
     * @param mapper     mapeador dominio ⇄ DTO (obligatorio)
     */
    public EnterpriseDashboardController(EnterpriseProjectRepository repository,
                                         EnterpriseWebMapper mapper) {
        this.repository = require(repository, "repository");
        this.mapper = require(mapper, "mapper");
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
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
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
        return ResponseEntity.ok(mapper.toDashboard(project, versions));
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
