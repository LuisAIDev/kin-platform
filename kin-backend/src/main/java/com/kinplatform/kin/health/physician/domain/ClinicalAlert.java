package com.kinplatform.kin.health.physician.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Alerta clínica para el portal de médicos (ADR-031).
 *
 * <p>Generada de forma determinista cuando un triaje alcanza urgencia ALTA
 * (p. ej. síntomas de infarto) o por reglas configuradas. El médico puede
 * marcarla como atendida ({@code ACKNOWLEDGED}).</p>
 */
public record ClinicalAlert(
        UUID id,
        UUID patientId,
        UUID physicianId,
        AlertType type,
        AlertSeverity severity,
        String message,
        AlertStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime acknowledgedAt) {

    public ClinicalAlert {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId no puede ser null");
        }
        if (physicianId == null) {
            throw new IllegalArgumentException("physicianId no puede ser null");
        }
        type = type == null ? AlertType.HIGH_URGENCY_TRIAGE : type;
        severity = severity == null ? AlertSeverity.ALTA : severity;
        message = message == null ? "" : message;
        status = status == null ? AlertStatus.PENDING : status;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static ClinicalAlert of(
            UUID id,
            UUID patientId,
            UUID physicianId,
            AlertType type,
            AlertSeverity severity,
            String message,
            AlertStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime acknowledgedAt) {
        return new ClinicalAlert(
                id, patientId, physicianId, type, severity, message, status, createdAt, acknowledgedAt);
    }

    public boolean isActive() {
        return status == AlertStatus.PENDING;
    }

    public enum AlertType {
        HIGH_URGENCY_TRIAGE,
        GENERAL
    }

    public enum AlertSeverity {
        ALTA,
        MEDIA,
        BAJA
    }

    public enum AlertStatus {
        PENDING,
        ACKNOWLEDGED
    }
}
