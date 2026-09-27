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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diagnoses",
       indexes = {
           @Index(name = "idx_diagnoses_encounter_id", columnList = "encounter_id"),
           @Index(name = "idx_diagnoses_patient_id", columnList = "patient_id"),
           @Index(name = "idx_diagnoses_cie10_code", columnList = "cie10_code"),
           @Index(name = "idx_diagnoses_type_status", columnList = "diagnosis_type, status")
       })
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "cie10_code", nullable = false, length = 10)
    private String cie10Code;

    @Column(name = "cie10_description", length = 500)
    private String cie10Description;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", nullable = false, length = 20)
    private DiagnosisType diagnosisType;

    @Enumerated(EnumType.STRING)
    @Column(name = "certainty", nullable = false, length = 20)
    private Certainty certainty;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 20)
    private Classification classification;

    @Column(name = "supported_by", length = 500)
    private String supportedBy;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolution_date")
    private LocalDate resolutionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private Status status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum DiagnosisType {
        PRINCIPAL, SECUNDARIO, COMORBILIDAD, COMPLICACION, INGRESO, EGRESO
    }

    public enum Certainty {
        CONFIRMED, PRESUMPTIVE, RULED_OUT, WORKING
    }

    public enum Classification {
        CONSULTA, INGRESO, EGRESO, INTERCONSULTA, URGENCIA
    }

    public enum Status {
        ACTIVE, RESOLVED, CHRONIC, IN_REMISSION
    }
}