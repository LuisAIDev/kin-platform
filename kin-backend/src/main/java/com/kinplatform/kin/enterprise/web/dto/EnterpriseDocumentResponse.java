package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Documento de una versión del proyecto empresarial (Fase 10, Milestone 2I).
 *
 * <p>Metadatos de un {@code DocumentArtifact}: identidad, tipo, tamaño en bytes,
 * trazabilidad del motor que lo generó y formato de renderizado objetivo. El
 * contenido se sirve a través de los endpoints de exportación binaria, no se
 * expone en este DTO.</p>
 *
 * @param id            identificador único del documento
 * @param type          tipo de documento ({@code DocumentType})
 * @param size          tamaño del contenido en bytes
 * @param createdAt     instante de generación del documento
 * @param generatedBy   motor de dominio que generó el documento
 * @param engineVersion versión del motor
 * @param version       versión del proyecto empresarial a la que pertenece
 * @param inputHash     hash de la entrada que produjo el documento
 * @param renderFormat  formato de renderizado objetivo, o {@code null}
 * @param mimeType      tipo MIME del contenido, o {@code null}
 * @param checksum      checksum del contenido, o {@code null}
 */
@Schema(description = "Documento de una versión del proyecto empresarial")
public record EnterpriseDocumentResponse(
    @NotNull(message = "'id' no puede ser null")
    @Schema(description = "Identificador único del documento", example = "9f2a6c51-2d1e-4a1f-b2c3-d4e5f6a7b8c9")
    UUID id,

    @NotBlank(message = "'type' no puede estar vacío")
    @Schema(description = "Tipo de documento", example = "LEAN_CANVAS",
        allowableValues = {"EXECUTIVE_REPORT", "LEAN_CANVAS", "DOFA", "FINANCIAL_PLAN",
            "MARKET_PLAN", "ROADMAP", "RISK_MATRIX", "KPI", "INNOVATION_PLAN"})
    String type,

    @PositiveOrZero(message = "'size' no puede ser negativo")
    @Schema(description = "Tamaño del contenido en bytes", example = "1840")
    long size,

    @NotNull(message = "'createdAt' no puede ser null")
    @Schema(description = "Instante de generación del documento", example = "2026-08-02T10:16:00Z")
    OffsetDateTime createdAt,

    @NotBlank(message = "'generatedBy' no puede estar vacío")
    @Schema(description = "Motor de dominio que generó el documento", example = "BusinessModelEngine")
    String generatedBy,

    @NotBlank(message = "'engineVersion' no puede estar vacío")
    @Schema(description = "Versión del motor", example = "1.0.0")
    String engineVersion,

    @Positive(message = "'version' debe ser mayor o igual a 1")
    @Schema(description = "Versión del proyecto empresarial a la que pertenece", example = "1")
    int version,

    @NotBlank(message = "'inputHash' no puede estar vacío")
    @Schema(description = "Hash de la entrada que produjo el documento", example = "sha256:1a2b3c4d")
    String inputHash,

    @Schema(description = "Formato de renderizado objetivo", example = "PDF",
        allowableValues = {"PDF", "DOCX", "PPTX"})
    String renderFormat,

    @Schema(description = "Tipo MIME del contenido", example = "text/plain")
    String mimeType,

    @Schema(description = "Checksum del contenido", example = "checksum-LEAN_CANVAS")
    String checksum
) {
}
