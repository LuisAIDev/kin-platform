package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo de error de la API Enterprise (Fase 10, Milestone 2I).
 *
 * <p>Respuesta uniforme de los errores HTTP producidos por el
 * {@code EnterpriseApiExceptionHandler}: instante, estado, código, mensaje,
 * ruta y, en errores de validación, los errores por campo.</p>
 *
 * @param timestamp   instante en que ocurrió el error
 * @param status      código de estado HTTP
 * @param error       frase de razón del estado HTTP
 * @param message     mensaje descriptivo del error
 * @param path        ruta de la solicitud que falló
 * @param fieldErrors errores de validación por campo (vacío si no aplica)
 */
@Schema(description = "Cuerpo de error de la API Enterprise")
public record EnterpriseApiError(
    @NotNull(message = "'timestamp' no puede ser null")
    @Schema(description = "Instante en que ocurrió el error", example = "2026-08-02T10:20:00Z")
    Instant timestamp,

    @Positive(message = "'status' debe ser un código HTTP válido")
    @Schema(description = "Código de estado HTTP", example = "404")
    int status,

    @NotBlank(message = "'error' no puede estar vacío")
    @Schema(description = "Frase de razón del estado HTTP", example = "Not Found")
    String error,

    @NotBlank(message = "'message' no puede estar vacío")
    @Schema(description = "Mensaje descriptivo del error", example = "No existe la versión 5 del proyecto 3fa85f64-...")
    String message,

    @NotBlank(message = "'path' no puede estar vacío")
    @Schema(description = "Ruta de la solicitud que falló", example = "/enterprise/3fa85f64-5717-4562-b3fc-2c963f66afa6/5")
    String path,

    @Schema(description = "Errores de validación por campo (vacío si no aplica)",
        example = "{\"async\":\"'async' es obligatorio\"}")
    Map<String, String> fieldErrors
) {

    public EnterpriseApiError {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }
}
