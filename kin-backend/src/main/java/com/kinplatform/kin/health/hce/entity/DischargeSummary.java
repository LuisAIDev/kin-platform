package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "discharge_summaries",
       indexes = {
           @Index(name = "idx_discharge_summaries_patient_id", columnList = "patient_id"),
           @Index(name = "idx_discharge_summaries_admission_id", columnList = "admission_id"),
           @Index(name = "idx_discharge_summaries_physician_id", columnList = "attending_physician_id"),
           @Index(name = "idx_discharge_summaries_dates", columnList = "admission_date, discharge_date"),
           @Index(name = "idx_discharge_summaries_condition", columnList = "discharge_condition")
       })
public class DischargeSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "admission_id")
    private UUID admissionId;

    @Column(name = "attending_physician_id", nullable = false)
    private UUID attendingPhysicianId;

    @Column(name = "admission_date", nullable = false)
    private Instant admissionDate;

    @Column(name = "discharge_date", nullable = false)
    private Instant dischargeDate;

    @Column(name = "length_of_stay")
    private Duration lengthOfStay;

    @Column(name = "admission_diagnosis_cie10", length = 10)
    private String admissionDiagnosisCie10;

    @Column(name = "discharge_diagnosis_cie10", nullable = false, length = 10)
    private String dischargeDiagnosisCie10;

    @Column(name = "secondary_diagnoses_cie10", columnDefinition = "TEXT[]")
    private String[] secondaryDiagnosesCie10;

    @Column(name = "clinical_summary", nullable = false, columnDefinition = "TEXT")
    private String clinicalSummary;

    @Column(name = "procedures_performed", columnDefinition = "TEXT[]")
    private String[] proceduresPerformed;

    @Column(name = "complications", columnDefinition = "TEXT[]")
    private String[] complications;

    @Enumerated(EnumType.STRING)
    @Column(name = "discharge_condition", length = 30)
    private DischargeCondition dischargeCondition;

    @Column(name = "discharge_disposition", length = 50)
    private String dischargeDisposition;

    @Column(name = "discharge_medications", columnDefinition = "jsonb")
    private String dischargeMedications;

    @Column(name = "followup_appointments", columnDefinition = "jsonb")
    private String followupAppointments;

    @Column(name = "alarm_signs", columnDefinition = "TEXT[]")
    private String[] alarmSigns;

    @Column(name = "general_recommendations", columnDefinition = "TEXT")
    private String generalRecommendations;

    @Column(name = "physician_signature_hash", length = 500)
    private String physicianSignatureHash;

    @Column(name = "signed_at")
    private Instant signedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum DischargeCondition {
        STABLE, IMPROVED, UNCHANGED, WORSENED, DECEASED, TRANSFERRED
    }
}