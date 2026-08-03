package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.application.EnterpriseDocumentBundle;
import com.kinplatform.kin.enterprise.application.EnterpriseExportOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationRequest;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseDocumentResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseExportResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseGenerateRequest;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseProjectResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseProjectSummaryResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseStatusResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controlador REST del módulo Enterprise (Fase 10, Milestone 2I).
 *
 * <p>Expone la API del proyecto empresarial bajo {@code /api/v1/enterprise}
 * (el prefijo {@code /api/v1} lo aporta la configuración del servidor):
 * consulta de versiones, generación, estado, documentos y exportación en PDF,
 * DOCX y PPTX. Es un adaptador de entrada de la arquitectura hexagonal: no
 * contiene lógica de negocio; únicamente valida la entrada, delega en los
 * orquestadores de la capa de aplicación y traduce los resultados a DTO y
 * códigos HTTP.</p>
 */
@RestController
@RequestMapping("/enterprise")
@Tag(name = "Enterprise", description = "API del módulo Enterprise: consulta, "
    + "generación y exportación de los documentos del proyecto empresarial.")
public class EnterpriseController {

    private final EnterpriseProjectRepository repository;
    private final ContextRepository contextRepository;
    private final EnterpriseExportOrchestrator exportOrchestrator;
    private final EnterpriseGenerationOrchestrator generationOrchestrator;
    private final EnterpriseWebMapper mapper;

    /**
     * @param repository              puerto de persistencia del proyecto empresarial
     * @param contextRepository       puerto del contexto durable de conversación
     * @param exportOrchestrator      fachada de exportación de documentos
     * @param generationOrchestrator  fachada de generación del proyecto empresarial
     * @param mapper                  mapeador dominio ⇄ DTO
     */
    public EnterpriseController(EnterpriseProjectRepository repository,
                                ContextRepository contextRepository,
                                EnterpriseExportOrchestrator exportOrchestrator,
                                EnterpriseGenerationOrchestrator generationOrchestrator,
                                EnterpriseWebMapper mapper) {
        this.repository = require(repository, "repository");
        this.contextRepository = require(contextRepository, "contextRepository");
        this.exportOrchestrator = require(exportOrchestrator, "exportOrchestrator");
        this.generationOrchestrator = require(generationOrchestrator, "generationOrchestrator");
        this.mapper = require(mapper, "mapper");
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @GetMapping("/{projectId}")
    @Operation(summary = "Resumen de la última versión",
        description = "Devuelve un resumen ligero (identidad, estado y número de "
            + "documentos) de la versión más reciente del proyecto empresarial.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumen de la última versión",
            content = @Content(schema = @Schema(implementation = EnterpriseProjectSummaryResponse.class))),
        @ApiResponse(responseCode = "404", description = "El proyecto no tiene versiones enterprise",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseProjectSummaryResponse> getSummary(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId) {
        EnterpriseProject project = repository.findLatestVersion(projectId)
            .orElseThrow(() -> new EnterpriseNotFoundException(
                "El proyecto " + projectId + " no tiene versiones enterprise."));
        return ResponseEntity.ok(mapper.toSummary(project));
    }

    @GetMapping("/{projectId}/latest")
    @Operation(summary = "Detalle de la última versión",
        description = "Devuelve la representación completa de la versión más "
            + "reciente del proyecto empresarial, incluidos sus documentos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Última versión completa",
            content = @Content(schema = @Schema(implementation = EnterpriseProjectResponse.class))),
        @ApiResponse(responseCode = "404", description = "El proyecto no tiene versiones enterprise",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseProjectResponse> getLatest(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId) {
        EnterpriseProject project = repository.findLatestVersion(projectId)
            .orElseThrow(() -> new EnterpriseNotFoundException(
                "El proyecto " + projectId + " no tiene versiones enterprise."));
        return ResponseEntity.ok(mapper.toResponse(project));
    }

    @GetMapping("/{projectId}/versions")
    @Operation(summary = "Listado de versiones",
        description = "Devuelve todas las versiones del proyecto empresarial "
            + "ordenadas de forma ascendente (200 con lista vacía si no hay versiones).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de versiones",
            content = @Content(array = @ArraySchema(
                schema = @Schema(implementation = EnterpriseVersionResponse.class))))
    })
    public ResponseEntity<List<EnterpriseVersionResponse>> getVersions(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId) {
        List<EnterpriseVersionResponse> versions = repository.findAllVersions(projectId).stream()
            .map(mapper::toVersion)
            .toList();
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{projectId}/{version}")
    @Operation(summary = "Detalle de una versión concreta",
        description = "Devuelve la representación completa de la versión "
            + "solicitada del proyecto empresarial, incluidos sus documentos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Versión completa",
            content = @Content(schema = @Schema(implementation = EnterpriseProjectResponse.class))),
        @ApiResponse(responseCode = "400", description = "Versión inválida (menor que 1)",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class))),
        @ApiResponse(responseCode = "404", description = "La versión solicitada no existe",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseProjectResponse> getByVersion(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version) {
        requireValidVersion(version);
        return ResponseEntity.ok(mapper.toResponse(requireVersion(projectId, version)));
    }

    // ------------------------------------------------------------------
    // Generación
    // ------------------------------------------------------------------

    @PostMapping("/{projectId}/generate")
    @Operation(summary = "Generar el proyecto empresarial",
        description = "Genera el proyecto empresarial a partir del contexto durable "
            + "de la conversación. Con 'async' false devuelve 201 Created con la versión "
            + "generada (409 si ya hay una generación en curso, 422 si falló o no hay "
            + "contexto). Con 'async' true devuelve 202 Accepted y delega la generación. "
            + "Si se indica 'requestedVersion' se regenera esa versión concreta.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Versión generada correctamente",
            content = @Content(schema = @Schema(implementation = EnterpriseProjectResponse.class))),
        @ApiResponse(responseCode = "202", description = "Generación aceptada (ejecución asíncrona)"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida (validación o formato)",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class))),
        @ApiResponse(responseCode = "409", description = "Ya existe una generación en curso",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class))),
        @ApiResponse(responseCode = "422", description = "No se puede generar (sin contexto o generación fallida)",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseProjectResponse> generate(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Modo de generación",
            required = true,
            content = @Content(schema = @Schema(implementation = EnterpriseGenerateRequest.class),
                examples = @ExampleObject(name = "Generación bloqueante",
                    summary = "Generación síncrona de la siguiente versión",
                    value = "{\"async\":false}")))
        @Valid @RequestBody EnterpriseGenerateRequest request) {
        ProjectContext context = contextRepository.find(projectId)
            .orElseThrow(() -> new EnterpriseUnprocessableEntityException(
                "El proyecto " + projectId + " no tiene contexto de conversación para generar."));
        EnterpriseGenerationRequest domain = mapper.toDomain(projectId, request, context);

        if (request.requestedVersion() != null) {
            return generationStatus(
                generationOrchestrator.generateRequested(domain, request.requestedVersion()));
        }
        if (request.asyncRequested()) {
            generationOrchestrator.generateAsync(domain);
            return ResponseEntity.accepted().build();
        }
        return generationStatus(generationOrchestrator.generate(domain));
    }

    // ------------------------------------------------------------------
    // Estado y documentos
    // ------------------------------------------------------------------

    @GetMapping("/{projectId}/{version}/status")
    @Operation(summary = "Estado de una versión",
        description = "Devuelve el estado del ciclo de vida de la generación de "
            + "una versión (REQUESTED, RUNNING, COMPLETED o FAILED).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado de la versión",
            content = @Content(schema = @Schema(implementation = EnterpriseStatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "La versión solicitada no existe",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseStatusResponse> getStatus(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version) {
        requireValidVersion(version);
        return ResponseEntity.ok(mapper.toStatus(requireVersion(projectId, version)));
    }

    @GetMapping("/{projectId}/{version}/documents")
    @Operation(summary = "Listado de documentos de una versión",
        description = "Devuelve los metadatos de los documentos generados en la "
            + "versión (sin contenido; el contenido se sirve por exportación).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de documentos",
            content = @Content(array = @ArraySchema(
                schema = @Schema(implementation = EnterpriseDocumentResponse.class)))),
        @ApiResponse(responseCode = "404", description = "La versión solicitada no existe",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<List<EnterpriseDocumentResponse>> getDocuments(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version) {
        requireValidVersion(version);
        return ResponseEntity.ok(mapper.toDocuments(requireVersion(projectId, version).documents()));
    }

    @GetMapping("/{projectId}/{version}/documents/{type}")
    @Operation(summary = "Metadatos de un documento concreto",
        description = "Devuelve los metadatos del documento del tipo solicitado "
            + "dentro de la versión indicada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Metadatos del documento",
            content = @Content(schema = @Schema(implementation = EnterpriseDocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Tipo de documento inválido",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class))),
        @ApiResponse(responseCode = "404", description = "Versión o documento no encontrado",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseDocumentResponse> getDocument(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version,
        @Parameter(name = "type", description = "Tipo de documento", required = true,
            example = "LEAN_CANVAS", in = ParameterIn.PATH)
        @PathVariable DocumentType type) {
        requireValidVersion(version);
        EnterpriseProject project = requireVersion(projectId, version);
        DocumentArtifact artifact = project.findDocument(type)
            .orElseThrow(() -> new EnterpriseNotFoundException(
                "La versión " + version + " no contiene un documento de tipo " + type + "."));
        return ResponseEntity.ok(mapper.toDocument(artifact));
    }

    // ------------------------------------------------------------------
    // Exportación
    // ------------------------------------------------------------------

    @GetMapping("/{projectId}/{version}/export")
    @Operation(summary = "Resumen de exportación de una versión",
        description = "Devuelve las representaciones binarias disponibles por tipo "
            + "de documento y formato, con el tamaño en bytes de cada una.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumen de exportación",
            content = @Content(schema = @Schema(implementation = EnterpriseExportResponse.class))),
        @ApiResponse(responseCode = "404", description = "La versión solicitada no existe",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<EnterpriseExportResponse> getExport(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version) {
        requireValidVersion(version);
        return ResponseEntity.ok(mapper.toExport(exportOrchestrator.export(projectId, version)));
    }

    @GetMapping("/{projectId}/{version}/export/{format}")
    @Operation(summary = "Exportar todos los documentos en un formato",
        description = "Descarga un archivo ZIP con un documento por entrada "
            + "(nombre: tipo de documento) renderizado en el formato solicitado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ZIP con los documentos del formato solicitado",
            content = @Content(mediaType = "application/zip")),
        @ApiResponse(responseCode = "400", description = "Formato inválido",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class))),
        @ApiResponse(responseCode = "404", description = "La versión solicitada no existe",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<byte[]> exportFormat(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version,
        @Parameter(name = "format", description = "Formato de salida", required = true,
            example = "PDF", in = ParameterIn.PATH)
        @PathVariable RenderFormat format) {
        requireValidVersion(version);
        byte[] zip = zipDocuments(exportOrchestrator.export(projectId, version), format);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"enterprise-" + version + "-" + format.name().toLowerCase() + ".zip\"")
            .body(zip);
    }

    @GetMapping("/{projectId}/{version}/export/{type}/{format}")
    @Operation(summary = "Exportar un documento en un formato",
        description = "Descarga el contenido binario de un documento concreto de la "
            + "versión, renderizado en el formato solicitado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento renderizado",
            content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "400", description = "Tipo de documento o formato inválidos",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class))),
        @ApiResponse(responseCode = "404", description = "Versión, documento o formato no disponibles",
            content = @Content(schema = @Schema(implementation = com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError.class)))
    })
    public ResponseEntity<byte[]> exportDocument(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen", required = true,
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial", required = true,
            example = "1", in = ParameterIn.PATH)
        @PathVariable int version,
        @Parameter(name = "type", description = "Tipo de documento", required = true,
            example = "LEAN_CANVAS", in = ParameterIn.PATH)
        @PathVariable DocumentType type,
        @Parameter(name = "format", description = "Formato de salida", required = true,
            example = "PDF", in = ParameterIn.PATH)
        @PathVariable RenderFormat format) {
        requireValidVersion(version);
        byte[] bytes = exportOrchestrator.export(projectId, version)
            .rendering(type, format)
            .orElseThrow(() -> new EnterpriseNotFoundException(
                "La versión " + version + " no contiene el documento " + type
                    + " en formato " + format + "."));
        return ResponseEntity.ok()
            .contentType(contentType(format))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + type.name().toLowerCase() + "." + extension(format) + "\"")
            .body(bytes);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    /**
     * Traduce el resultado de una generación a su código HTTP: 201 Created para
     * una versión completada, 409 Conflict si la generación está en curso y 422
     * Unprocessable Entity si la generación terminó en fallo.
     */
    private ResponseEntity<EnterpriseProjectResponse> generationStatus(EnterpriseProject project) {
        if (project.isRequested() || project.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapper.toResponse(project));
        }
        if (project.isFailed()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapper.toResponse(project));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(project));
    }

    private EnterpriseProject requireVersion(UUID projectId, int version) {
        return repository.findByVersion(projectId, version)
            .orElseThrow(() -> new EnterpriseNotFoundException(
                "No existe la versión " + version + " del proyecto " + projectId + "."));
    }

    private void requireValidVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException(
                "La versión debe ser mayor o igual a 1 (recibida: " + version + ").");
        }
    }

    /**
     * Empaqueta en un ZIP las representaciones del formato dado, con una entrada
     * por documento (nombre: tipo de documento en minúsculas).
     */
    private byte[] zipDocuments(EnterpriseDocumentBundle bundle, RenderFormat format) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<DocumentType, Map<RenderFormat, byte[]>> entry : bundle.documents().entrySet()) {
                byte[] bytes = entry.getValue().get(format);
                if (bytes == null) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(entry.getKey().name().toLowerCase() + "." + extension(format)));
                zip.write(bytes);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo empaquetar la exportación en ZIP", e);
        }
        return out.toByteArray();
    }

    private static MediaType contentType(RenderFormat format) {
        return switch (format) {
            case PDF -> MediaType.parseMediaType("application/pdf");
            case DOCX -> MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case PPTX -> MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        };
    }

    private static String extension(RenderFormat format) {
        return format.name().toLowerCase();
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null");
        }
        return value;
    }
}
