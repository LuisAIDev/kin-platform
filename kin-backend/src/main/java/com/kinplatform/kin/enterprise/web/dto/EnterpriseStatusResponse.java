package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Estado de una versión del proyecto empresarial (Fase 10, Milestone 2I).
 *
 * <p>Representación del ciclo de vida de la generación usada por
 * {@code GET /enterprise/{projectId}/{version}/status}.</p>
 *
 * @param projectId    identificador del proyecto de KIN origen
 * @param version      número de la versión
 * @param status       estado de la generación
 * @param updatedAt    instante de la última actualización
 * @param completedAt  instante de finalización, o {@code null}
 * @param failedReason motivo del fallo, o {@code null}
 */
@Schema(description = "Estado de la generación de una versión del proyecto empresarial")
public record EnterpriseStatusResponse(
    @NotNull(message = "'projectId' no puede ser null")
    @Schema(description = "Identificador del proyecto de KIN origen", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID projectId,

    @Positive(message = "'version' debe ser mayor o igual a 1")
    @Schema(description = "Número de la versión", example = "1")
    int version,

    @NotBlank(message = "'status' no puede estar vacío")
    @Schema(description = "Estado de la generación", example = "RUNNING",
        allowableValues = {"REQUESTED", "RUNNING", "COMPLETED", "FAILED"})
    String status,

    @NotNull(message = "'updatedAt' no puede ser null")
    @Schema(description = "Instante de la última actualización", example = "2026-08-02T10:16:00Z")
    OffsetDateTime updatedAt,

    @Schema(description = "Instante de finalización (solo COMPLETED)", example = "2026-08-02T10:16:00Z")
    OffsetDateTime completedAt,

    @Schema(description = "Motivo del fallo (solo FAILED)", example = "El motor de mercado falló")
    String failedReason
) {
}
