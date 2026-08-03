package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dashboard de una versión del proyecto empresarial (Fase 10, Milestone 2J).
 *
 * <p>Vista consolidada para la pantalla Enterprise Dashboard: estado, progreso,
 * documentos, score, fechas, versiones y estadísticas de la versión.</p>
 *
 * @param projectId               identificador del proyecto de KIN origen
 * @param version                 versión consultada
 * @param status                  estado de la generación
 * @param progress                progreso porcentual (0-100)
 * @param documentCount           número de documentos de la versión
 * @param versionsCount           número total de versiones del proyecto
 * @param createdAt               instante de creación de la versión
 * @param updatedAt               instante de la última actualización
 * @param completedAt             instante de finalización, o {@code null}
 * @param failedReason            motivo del fallo, o {@code null}
 * @param generationDurationMillis duración de la generación, o {@code null}
 * @param score                   Enterprise Score, o {@code null} si no está disponible
 * @param documents               documentos de la versión
 * @param versions                versiones del proyecto (ascendentes)
 * @param statistics              estadísticas (bytes totales, recuentos, duración)
 */
@Schema(description = "Dashboard de una versión del proyecto empresarial")
public record EnterpriseDashboardResponse(
    @NotNull @Schema(description = "Identificador del proyecto de KIN origen",
        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID projectId,

    @Positive @Schema(description = "Versión consultada", example = "1")
    int version,

    @NotBlank @Schema(description = "Estado de la generación", example = "COMPLETED")
    String status,

    @PositiveOrZero @Schema(description = "Progreso porcentual (0-100)", example = "100")
    int progress,

    @PositiveOrZero @Schema(description = "Número de documentos de la versión", example = "7")
    int documentCount,

    @PositiveOrZero @Schema(description = "Número total de versiones", example = "3")
    int versionsCount,

    @NotNull @Schema(description = "Instante de creación", example = "2026-08-02T10:15:30Z")
    OffsetDateTime createdAt,

    @NotNull @Schema(description = "Instante de la última actualización", example = "2026-08-02T10:16:00Z")
    OffsetDateTime updatedAt,

    @Schema(description = "Instante de finalización (solo COMPLETED)", example = "2026-08-02T10:16:00Z")
    OffsetDateTime completedAt,

    @Schema(description = "Motivo del fallo (solo FAILED)", example = "El motor de mercado falló")
    String failedReason,

    @PositiveOrZero @Schema(description = "Duración de la generación en milisegundos", example = "30000")
    Long generationDurationMillis,

    @Schema(description = "Enterprise Score (null si no está disponible)")
    EnterpriseScoreSection score,

    @NotNull @Schema(description = "Documentos de la versión")
    List<EnterpriseDocumentResponse> documents,

    @NotNull @Schema(description = "Versiones del proyecto (ascendentes)")
    List<EnterpriseVersionResponse> versions,

    @NotNull @Schema(description = "Estadísticas del dashboard",
        example = "{\"documentCount\":7,\"versionsCount\":3,\"totalBytes\":12840,\"generationDurationMs\":30000}")
    Map<String, Long> statistics
) {
}
