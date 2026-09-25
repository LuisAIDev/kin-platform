package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.Duration;
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
@Table(name = "treatment_plans",
       indexes = {
           @Index(name = "idx_treatment_plans_encounter_id", columnList = "encounter_id"),
           @Index(name = "idx_treatment_plans_patient_id", columnList = "patient_id"),
           @Index(name = "idx_treatment_plans_physician_id", columnList = "physician_id")
       })
public class TreatmentPlan {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "conduct", nullable = false, length = 30)
    private Conduct conduct;

    @Column(name = "therapeutic_goals", columnDefinition = "TEXT[]")
    private String[] therapeuticGoals;

    @Column(name = "followup_plan", length = 5000)
    private String followupPlan;

    @Column(name = "reevaluation_criteria", length = 2000)
    private String reevaluationCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "prognosis", length = 20)
    private Prognosis prognosis;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "estimated_duration")
    private Duration estimatedDuration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Conduct {
        OBSERVATION, OUTPATIENT_TREATMENT, REFERRAL, HOSPITALIZATION, SURGERY, PALLIATIVE, REHABILITATION
    }

    public enum Prognosis {
        EXCELLENT, GOOD, FAIR, POOR, GUARDED, UNKNOWN
    }
}