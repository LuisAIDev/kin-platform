package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
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
@Table(name = "physical_exam",
       indexes = {
           @Index(name = "idx_physical_exam_encounter_id", columnList = "encounter_id"),
           @Index(name = "idx_physical_exam_patient_id", columnList = "patient_id"),
           @Index(name = "idx_physical_exam_recorded_at", columnList = "recorded_at")
       })
public class PhysicalExam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "evolution_id")
    private UUID evolutionId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "recorded_at", nullable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();

    @Column(name = "bp_systolic")
    private Integer bpSystolic;

    @Column(name = "bp_diastolic")
    private Integer bpDiastolic;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "bmi", precision = 4, scale = 2, insertable = false, updatable = false)
    private BigDecimal bmi;

    @Column(name = "glasgow_score")
    private Integer glasgowScore;

    @Column(name = "pain_scale")
    private Integer painScale;

    @Enumerated(EnumType.STRING)
    @Column(name = "pain_scale_type", length = 20)
    @Builder.Default
    private PainScaleType painScaleType = PainScaleType.EVA;

    @Column(name = "general_appearance", length = 100)
    private String generalAppearance;

    @Column(name = "head_neck", length = 2000)
    private String headNeck;

    @Column(name = "cardiovascular", length = 2000)
    private String cardiovascular;

    @Column(name = "respiratory", length = 2000)
    private String respiratory;

    @Column(name = "abdominal", length = 2000)
    private String abdominal;

    @Column(name = "neurological", length = 2000)
    private String neurological;

    @Column(name = "musculoskeletal", length = 2000)
    private String musculoskeletal;

    @Column(name = "skin", length = 2000)
    private String skin;

    @Column(name = "genitourinary", length = 2000)
    private String genitourinary;

    @Column(name = "psychiatric", length = 2000)
    private String psychiatric;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validated_scales", columnDefinition = "jsonb")
    @Builder.Default
    private String validatedScales = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum PainScaleType {
        EVA, FLACC, NIPS, PAINAD, CRIES
    }
}