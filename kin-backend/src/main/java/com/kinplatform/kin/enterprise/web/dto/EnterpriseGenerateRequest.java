package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud de generación del proyecto empresarial (Fase 10, Milestone 2I).
 *
 * <p>Define el modo de ejecución de {@code POST /enterprise/{projectId}/generate}:
 * {@code async} indica si la generación se delega de forma asíncrona (HTTP 202
 * Accepted) o se espera de forma bloqueante (HTTP 201 Created), y
 * {@code requestedVersion} permite solicitar la generación de una versión
 * concreta (re-generación) en lugar de la siguiente versión.</p>
 *
 * @param async            {@code true} para ejecución asíncrona (obligatorio)
 * @param requestedVersion versión concreta a generar (opcional)
 */
@Schema(description = "Solicitud de generación del proyecto empresarial")
public record EnterpriseGenerateRequest(
    @NotNull(message = "'async' es obligatorio (true = asíncrono, false = bloqueante)")
    @Schema(description = "true ejecuta la generación de forma asíncrona (202 Accepted); "
        + "false la ejecuta de forma bloqueante (201 Created)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean async,

    @Positive(message = "'requestedVersion' debe ser mayor o igual a 1")
    @Schema(description = "Versión concreta a generar (re-generación). Si se omite, se genera la siguiente versión.",
        example = "2")
    Integer requestedVersion
) {

    /**
     * Indica si la generación debe ejecutarse de forma asíncrona.
     */
    public boolean asyncRequested() {
        return Boolean.TRUE.equals(async);
    }
}
