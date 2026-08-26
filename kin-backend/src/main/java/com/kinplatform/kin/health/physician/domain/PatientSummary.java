package com.kinplatform.kin.health.physician.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resumen clínico de un paciente para el portal de médicos (ADR-031).
 *
 * <p>Vista médica agregada a partir del historial de triajes y el perfil del
 * paciente: condiciones activas/más frecuentes, factores de riesgo, últimos
 * triajes y fecha del último. Generado de forma determinista por
 * {@code PhysicianService}.</p>
 */
public record PatientSummary(
        UUID patientId,
        String patientName,
        List<String> activeConditions,
        List<String> riskFactors,
        List<String> chronicConditions,
        int totalTriages,
        OffsetDateTime lastTriageAt,
        int activeAlerts) {

    public PatientSummary {
        activeConditions = activeConditions == null ? List.of() : List.copyOf(activeConditions);
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        chronicConditions = chronicConditions == null ? List.of() : List.copyOf(chronicConditions);
    }
}
