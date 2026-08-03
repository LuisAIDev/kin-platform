package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;
import java.util.UUID;

/**
 * Resumen de exportación de una versión del proyecto empresarial (Fase 10,
 * Milestone 2I).
 *
 * <p>Describe las representaciones binarias disponibles en
 * {@code GET /enterprise/{projectId}/{version}/export}: para cada tipo de
 * documento, los formatos de salida soportados con el tamaño en bytes de cada
 * representación. El contenido binario se sirve en los endpoints
 * {@code /export/{format}} y {@code /export/{type}/{format}}.</p>
 *
 * @param projectId identificador del proyecto de KIN origen
 * @param version   versión exportada
 * @param documents mapa tipo de documento → formato → tamaño en bytes
 */
@Schema(description = "Resumen de exportación de una versión del proyecto empresarial")
public record EnterpriseExportResponse(
    @NotNull(message = "'projectId' no puede ser null")
    @Schema(description = "Identificador del proyecto de KIN origen", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID projectId,

    @Positive(message = "'version' debe ser mayor o igual a 1")
    @Schema(description = "Versión exportada", example = "1")
    int version,

    @NotNull(message = "'documents' no puede ser null")
    @Schema(description = "Representaciones disponibles por tipo de documento y formato (tamaño en bytes)",
        example = "{\"LEAN_CANVAS\":{\"PDF\":1840,\"DOCX\":2210,\"PPTX\":5120}}")
    Map<String, Map<String, @PositiveOrZero Long>> documents
) {
}
