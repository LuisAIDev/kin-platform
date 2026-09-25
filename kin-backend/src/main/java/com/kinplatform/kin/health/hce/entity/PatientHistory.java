package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_history",
       indexes = {
           @Index(name = "idx_patient_history_patient_id", columnList = "patient_id"),
           @Index(name = "idx_patient_history_type", columnList = "history_type"),
           @Index(name = "idx_patient_history_status", columnList = "status"),
           @Index(name = "idx_patient_history_patient_type", columnList = "patient_id, history_type")
       })
public class PatientHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_type", nullable = false, length = 30)
    private HistoryType historyType;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolution_date")
    private LocalDate resolutionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private Severity severity;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    @Builder.Default
    private String details = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum HistoryType {
        ALLERGY, SURGERY, MEDICATION, VACCINE, FAMILY, TOXICOLOGICAL, GYNECO_OBSTETRIC
    }

    public enum Status {
        ACTIVE, RESOLVED, CHRONIC, IN_REMISSION
    }

    public enum Severity {
        LEVE, MODERADO, GRAVE, CRITICO
    }
}