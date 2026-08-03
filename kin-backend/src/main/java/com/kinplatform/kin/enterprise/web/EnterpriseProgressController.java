package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Controlador SSE del progreso de generación del proyecto empresarial (Fase 10,
 * Milestone 2J).
 *
 * <p>Expone {@code GET /enterprise/{projectId}/{version}/stream} en
 * {@code text/event-stream}: suscribe al cliente al
 * {@link EnterpriseProgressService} y publica el estado actual de la versión
 * (si existe) como primer evento, de modo que el cliente conoce al instante el
 * progreso acumulado y luego recibe los eventos en vivo (REQUESTED, RUNNING,
 * DOCUMENT_GENERATED, COMPLETED o FAILED). El heartbeat lo gestiona el propio
 * servicio.</p>
 */
@RestController
@RequestMapping("/enterprise")
@Tag(name = "Enterprise", description = "API del módulo Enterprise: progreso en "
    + "tiempo real vía Server Sent Events de la generación del proyecto empresarial.")
public class EnterpriseProgressController {

    private final EnterpriseProgressService progressService;
    private final EnterpriseProgressPublisher publisher;
    private final EnterpriseProjectRepository repository;

    /**
     * @param progressService servicio de progreso SSE (obligatorio)
     * @param publisher       publicador de progreso (obligatorio)
     * @param repository      puerto de persistencia (obligatorio)
     */
    public EnterpriseProgressController(EnterpriseProgressService progressService,
                                        EnterpriseProgressPublisher publisher,
                                        EnterpriseProjectRepository repository) {
        this.progressService = require(progressService, "progressService");
        this.publisher = require(publisher, "publisher");
        this.repository = require(repository, "repository");
    }

    @GetMapping(value = "/{projectId}/{version}/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Flujo SSE del progreso de generación",
        description = "Suscribe al cliente al flujo de eventos de progreso de la "
            + "generación de una versión. Emite el estado actual como primer evento "
            + "(si la versión existe) y después los eventos en vivo: REQUESTED, "
            + "RUNNING, DOCUMENT_GENERATED por documento, COMPLETED o FAILED. La "
            + "conexión se cierra al alcanzar un estado terminal.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flujo text/event-stream de progreso",
            content = @Content(mediaType = "text/event-stream")),
        @ApiResponse(responseCode = "400", description = "Versión inválida")
    })
    public SseEmitter stream(
        @Parameter(name = "projectId", description = "Identificador del proyecto de KIN origen",
            required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", in = ParameterIn.PATH)
        @PathVariable UUID projectId,
        @Parameter(name = "version", description = "Versión del proyecto empresarial",
            required = true, example = "1", in = ParameterIn.PATH)
        @PathVariable int version) {
        requireValidVersion(version);
        SseEmitter emitter = progressService.subscribe(projectId, version);
        repository.findByVersion(projectId, version)
            .ifPresent(publisher::publishFor);
        return emitter;
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
