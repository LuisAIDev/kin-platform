package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resumen de la última versión del proyecto empresarial (Fase 10, Milestone 2I).
 *
 * <p>Representación ligera usada por {@code GET /enterprise/{projectId}}: solo
 * identidad, estado, número de documentos y timestamps, sin el detalle de cada
 * documento.</p>
 *
 * @param projectId     identificador del proyecto de KIN origen
 * @param version       versión más reciente
 * @param status        estado de la generación
 * @param documentCount número de documentos de la versión
 * @param updatedAt     instante de la última actualización
 */
@Schema(description = "Resumen de la última versión del proyecto empresarial")
public record EnterpriseProjectSummaryResponse(
    @NotNull(message = "'projectId' no puede ser null")
    @Schema(description = "Identificador del proyecto de KIN origen", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID projectId,

    @Positive(message = "'version' debe ser mayor o igual a 1")
    @Schema(description = "Versión más reciente", example = "3")
    int version,

    @NotBlank(message = "'status' no puede estar vacío")
    @Schema(description = "Estado de la generación", example = "COMPLETED")
    String status,

    @PositiveOrZero(message = "'documentCount' no puede ser negativo")
    @Schema(description = "Número de documentos de la versión", example = "7")
    int documentCount,

    @NotNull(message = "'updatedAt' no puede ser null")
    @Schema(description = "Instante de la última actualización", example = "2026-08-02T10:16:00Z")
    OffsetDateTime updatedAt
) {
}
