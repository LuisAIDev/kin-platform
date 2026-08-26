package com.kinplatform.kin.health.physician.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de alerta clínica (tabla {@code clinical_alerts}, ADR-031).
 */
@Entity
@Table(name = "clinical_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalAlertEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertSeverity severity;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertStatus.PENDING;
        }
    }
}
