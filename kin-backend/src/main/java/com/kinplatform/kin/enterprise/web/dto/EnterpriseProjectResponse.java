package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representación completa de una versión del proyecto empresarial (Fase 10,
 * Milestone 2I).
 *
 * <p>Expone la cabecera del aggregate {@code EnterpriseProject} (identidad,
 * estado, timestamps y motivo de fallo) junto con el inventario de documentos
 * de la versión. La puntuación Enterprise no forma parte del aggregate
 * (contrato congelado) y por tanto no se expone.</p>
 *
 * @param projectId     identificador del proyecto de KIN origen
 * @param version       versión del proyecto empresarial
 * @param status        estado de la generación ({@code REQUESTED},
 *                      {@code RUNNING}, {@code COMPLETED} o {@code FAILED})
 * @param createdAt     instante de creación de la versión
 * @param updatedAt     instante de la última actualización
 * @param completedAt   instante de finalización, o {@code null}
 * @param failedReason  motivo del fallo, o {@code null}
 * @param documentCount número de documentos de la versión
 * @param documents     documentos de la versión
 */
@Schema(description = "Versión completa del proyecto empresarial")
public record EnterpriseProjectResponse(
    @NotNull(message = "'projectId' no puede ser null")
    @Schema(description = "Identificador del proyecto de KIN origen", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID projectId,

    @Positive(message = "'version' debe ser mayor o igual a 1")
    @Schema(description = "Versión del proyecto empresarial", example = "1")
    int version,

    @NotBlank(message = "'status' no puede estar vacío")
    @Schema(description = "Estado de la generación", example = "COMPLETED",
        allowableValues = {"REQUESTED", "RUNNING", "COMPLETED", "FAILED"})
    String status,

    @NotNull(message = "'createdAt' no puede ser null")
    @Schema(description = "Instante de creación de la versión", example = "2026-08-02T10:15:30Z")
    OffsetDateTime createdAt,

    @NotNull(message = "'updatedAt' no puede ser null")
    @Schema(description = "Instante de la última actualización", example = "2026-08-02T10:16:00Z")
    OffsetDateTime updatedAt,

    @Schema(description = "Instante de finalización de la generación (solo COMPLETED)", example = "2026-08-02T10:16:00Z")
    OffsetDateTime completedAt,

    @Schema(description = "Motivo del fallo (solo FAILED)", example = "El motor de mercado falló")
    String failedReason,

    @PositiveOrZero(message = "'documentCount' no puede ser negativo")
    @Schema(description = "Número de documentos de la versión", example = "7")
    int documentCount,

    @NotNull(message = "'documents' no puede ser null")
    @Schema(description = "Documentos generados en la versión")
    List<EnterpriseDocumentResponse> documents
) {
}
