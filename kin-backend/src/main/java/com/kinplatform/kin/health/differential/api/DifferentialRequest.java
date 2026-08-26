package com.kinplatform.kin.health.differential.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request del diagnóstico diferencial por síntomas (ADR-029).
 *
 * @param symptoms    lista de síntomas (nombres del catálogo)
 * @param riskFactors factores de riesgo del paciente (p. ej. {@code fumador})
 */
public record DifferentialRequest(
        @NotEmpty(message = "Se requiere al menos un síntoma")
                @Size(max = 30, message = "Máximo 30 síntomas por consulta")
                List<@Size(max = 120) String> symptoms,
        @Size(max = 20, message = "Máximo 20 factores de riesgo") List<String> riskFactors) {

    public DifferentialRequest {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }
}
