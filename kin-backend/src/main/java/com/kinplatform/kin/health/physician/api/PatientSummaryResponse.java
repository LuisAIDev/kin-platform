package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.health.physician.domain.PatientSummary;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resumen de un paciente en el portal de médicos (ADR-031 + estado V30).
 */
public record PatientSummaryResponse(
        UUID patientId,
        String patientName,
        List<String> activeConditions,
        List<String> riskFactors,
        List<String> chronicConditions,
        int totalTriages,
        OffsetDateTime lastTriageAt,
        int activeAlerts,
        RelationshipStatus relationshipStatus) {

    public static PatientSummaryResponse from(PatientSummary summary) {
        return new PatientSummaryResponse(
                summary.patientId(),
                summary.patientName(),
                summary.activeConditions(),
                summary.riskFactors(),
                summary.chronicConditions(),
                summary.totalTriages(),
                summary.lastTriageAt(),
                summary.activeAlerts(),
                summary.relationshipStatus());
    }
}
