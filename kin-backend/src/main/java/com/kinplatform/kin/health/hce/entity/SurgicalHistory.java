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
@Table(name = "surgical_history",
       indexes = {
           @Index(name = "idx_surgical_history_patient_id", columnList = "patient_id"),
           @Index(name = "idx_surgical_history_date", columnList = "surgery_date"),
           @Index(name = "idx_surgical_history_cups", columnList = "procedure_cups_code"),
           @Index(name = "idx_surgical_history_patient_date", columnList = "patient_id, surgery_date")
       })
public class SurgicalHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "surgery_date", nullable = false)
    private LocalDate surgeryDate;

    @Column(name = "procedure_cups_code", nullable = false, length = 20)
    private String procedureCupsCode;

    @Column(name = "procedure_cups_description", length = 500)
    private String procedureCupsDescription;

    @Column(name = "diagnosis_cie10", length = 10)
    private String diagnosisCie10;

    @Column(name = "diagnosis_description", length = 500)
    private String diagnosisDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "surgery_type", length = 30)
    private SurgeryType surgeryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "anesthesia_type", length = 30)
    private AnesthesiaType anesthesiaType;

    @Column(name = "anesthesiologist_id")
    private UUID anesthesiologistId;

    @Column(name = "asa_classification")
    private Integer asaClassification;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "estimated_blood_loss_ml")
    private Integer estimatedBloodLossMl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "complications", columnDefinition = "jsonb")
    @Builder.Default
    private String complications = "[]";

    @Column(name = "surgeon_id")
    private UUID surgeonId;

    @Column(name = "assistant_surgeon_id")
    private UUID assistantSurgeonId;

    @Column(name = "institution", length = 200)
    private String institution;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SurgeryType {
        ELECTIVE, URGENT, EMERGENCY, AMBULATORY
    }

    public enum AnesthesiaType {
        GENERAL, REGIONAL_EPIDURAL, REGIONAL_SPINAL, REGIONAL_PLEXUS, LOCAL, SEDATION, NONE
    }
}