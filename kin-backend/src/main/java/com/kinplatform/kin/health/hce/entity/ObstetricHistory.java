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
@Table(name = "obstetric_history",
       indexes = {
           @Index(name = "idx_obstetric_history_patient_id", columnList = "patient_id"),
           @Index(name = "idx_obstetric_history_current", columnList = "current_pregnancy")
       })
public class ObstetricHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "gravida")
    @Builder.Default
    private Integer gravida = 0;

    @Column(name = "para")
    @Builder.Default
    private Integer para = 0;

    @Column(name = "abortions")
    @Builder.Default
    private Integer abortions = 0;

    @Column(name = "ectopic_pregnancies")
    @Builder.Default
    private Integer ectopicPregnancies = 0;

    @Column(name = "stillbirths")
    @Builder.Default
    private Integer stillbirths = 0;

    @Column(name = "living_children")
    @Builder.Default
    private Integer livingChildren = 0;

    @Column(name = "current_pregnancy")
    @Builder.Default
    private Boolean currentPregnancy = false;

    @Column(name = "lmp")
    private LocalDate lmp;

    @Column(name = "estimated_edd")
    private LocalDate estimatedEdd;

    @Column(name = "gestational_weeks")
    private Integer gestationalWeeks;

    @Column(name = "prenatal_controls")
    @Builder.Default
    private Integer prenatalControls = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_deliveries", columnDefinition = "jsonb")
    @Builder.Default
    private String previousDeliveries = "[]";

    @Column(name = "breastfeeding_status", length = 30)
    private String breastfeedingStatus;

    @Column(name = "breastfeeding_duration_months")
    private Integer breastfeedingDurationMonths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "obstetric_complications", columnDefinition = "jsonb")
    @Builder.Default
    private String obstetricComplications = "[]";

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
}