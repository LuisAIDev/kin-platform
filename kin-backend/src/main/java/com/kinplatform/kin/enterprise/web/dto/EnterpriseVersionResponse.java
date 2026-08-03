package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;

/**
 * Entrada del listado de versiones del proyecto empresarial (Fase 10,
 * Milestone 2I).
 *
 * <p>Representa una versión en {@code GET /enterprise/{projectId}/versions}:
 * identidad, estado, timestamps, motivo de fallo y número de documentos.</p>
 *
 * @param version       número de la versión
 * @param status        estado de la generación
 * @param createdAt     instante de creación de la versión
 * @param updatedAt     instante de la última actualización
 * @param completedAt   instante de finalización, o {@code null}
 * @param failedReason  motivo del fallo, o {@code null}
 * @param documentCount número de documentos de la versión
 */
@Schema(description = "Versión del proyecto empresarial (entrada de listado)")
public record EnterpriseVersionResponse(
    @Positive(message = "'version' debe ser mayor o igual a 1")
    @Schema(description = "Número de la versión", example = "2")
    int version,

    @NotBlank(message = "'status' no puede estar vacío")
    @Schema(description = "Estado de la generación", example = "COMPLETED")
    String status,

    @NotNull(message = "'createdAt' no puede ser null")
    @Schema(description = "Instante de creación de la versión", example = "2026-08-02T10:15:30Z")
    OffsetDateTime createdAt,

    @NotNull(message = "'updatedAt' no puede ser null")
    @Schema(description = "Instante de la última actualización", example = "2026-08-02T10:16:00Z")
    OffsetDateTime updatedAt,

    @Schema(description = "Instante de finalización (solo COMPLETED)", example = "2026-08-02T10:16:00Z")
    OffsetDateTime completedAt,

    @Schema(description = "Motivo del fallo (solo FAILED)", example = "El motor de mercado falló")
    String failedReason,

    @PositiveOrZero(message = "'documentCount' no puede ser negativo")
    @Schema(description = "Número de documentos de la versión", example = "7")
    int documentCount
) {
}
