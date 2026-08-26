package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Alerta clínica en el portal de médicos (ADR-031).
 */
public record ClinicalAlertResponse(
        UUID id,
        UUID patientId,
        String type,
        String severity,
        String message,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime acknowledgedAt) {

    public static ClinicalAlertResponse from(ClinicalAlert alert) {
        return new ClinicalAlertResponse(
                alert.id(),
                alert.patientId(),
                alert.type().name(),
                alert.severity().name(),
                alert.message(),
                alert.status().name(),
                alert.createdAt(),
                alert.acknowledgedAt());
    }
}
