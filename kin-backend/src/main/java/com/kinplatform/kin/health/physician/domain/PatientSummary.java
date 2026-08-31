package com.kinplatform.kin.health.physician.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resumen de un paciente para el portal de médicos (ADR-031, estado V30).
 *
 * <p>Vista médica agregada a partir del historial de triajes y el perfil del
 * paciente: condiciones activas/más frecuentes, factores de riesgo, últimos
 * triajes y fecha del último. Incluye el {@link RelationshipStatus} de la
 * relación médico-paciente (ACTIVE para la cartera actual; PENDING para las
 * invitaciones pendientes, donde los datos clínicos se muestran vacíos).
 * Generado de forma determinista por {@code PhysicianService}.</p>
 */
public record PatientSummary(
        UUID patientId,
        String patientName,
        List<String> activeConditions,
        List<String> riskFactors,
        List<String> chronicConditions,
        int totalTriages,
        OffsetDateTime lastTriageAt,
        int activeAlerts,
        RelationshipStatus relationshipStatus) {

    public PatientSummary {
        activeConditions = activeConditions == null ? List.of() : List.copyOf(activeConditions);
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        chronicConditions = chronicConditions == null ? List.of() : List.copyOf(chronicConditions);
        relationshipStatus = relationshipStatus == null ? RelationshipStatus.ACTIVE : relationshipStatus;
    }

    /** Resumen completo para una relación ACTIVA. */
    public static PatientSummary active(
            UUID patientId,
            String patientName,
            List<String> activeConditions,
            List<String> riskFactors,
            List<String> chronicConditions,
            int totalTriages,
            OffsetDateTime lastTriageAt,
            int activeAlerts) {
        return new PatientSummary(
                patientId,
                patientName,
                activeConditions,
                riskFactors,
                chronicConditions,
                totalTriages,
                lastTriageAt,
                activeAlerts,
                RelationshipStatus.ACTIVE);
    }

    /** Resumen de identidad para una relación PENDING (sin datos clínicos). */
    public static PatientSummary pending(UUID patientId, String patientName) {
        return new PatientSummary(patientId, patientName, List.of(), List.of(), List.of(), 0, null, 0,
                RelationshipStatus.PENDING);
    }
}
