package com.kinplatform.kin.health.dashboard.api;

import java.util.List;

/**
 * Request de actualización del perfil del paciente (ADR-030).
 *
 * @param riskFactors       factores de riesgo (fumador, diabetes, ...)
 * @param chronicConditions condiciones crónicas declaradas
 */
public record PatientProfileRequest(List<String> riskFactors, List<String> chronicConditions) {

    public PatientProfileRequest {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        chronicConditions = chronicConditions == null ? List.of() : List.copyOf(chronicConditions);
    }
}
