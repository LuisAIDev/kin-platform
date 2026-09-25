package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.time.Instant;
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
@Table(name = "referrals",
       indexes = {
           @Index(name = "idx_referrals_patient_id", columnList = "patient_id"),
           @Index(name = "idx_referrals_physician_id", columnList = "referring_physician_id"),
           @Index(name = "idx_referrals_status", columnList = "status"),
           @Index(name = "idx_referrals_type", columnList = "referral_type"),
           @Index(name = "idx_referrals_scheduled_at", columnList = "scheduled_at")
       })
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "referring_physician_id", nullable = false)
    private UUID referringPhysicianId;

    @Column(name = "referring_service", length = 100)
    private String referringService;

    @Column(name = "referred_to_service", nullable = false, length = 100)
    private String referredToService;

    @Column(name = "referred_to_institution", length = 200)
    private String referredToInstitution;

    @Column(name = "referred_to_physician_id")
    private UUID referredToPhysicianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_type", nullable = false, length = 30)
    private ReferralType referralType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.ROUTINE;

    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;

    @Column(name = "clinical_summary", length = 5000)
    private String clinicalSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "counterreferral_summary", length = 5000)
    private String counterreferralSummary;

    @Column(name = "counterreferral_recommendations", length = 5000)
    private String counterreferralRecommendations;

    @Column(name = "counterreferral_at")
    private Instant counterreferralAt;

    @Column(name = "counterreferral_by")
    private UUID counterreferralBy;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ReferralType {
        INTERCONSULTATION, COUNTERRREFERRAL, EMERGENCY, SECOND_OPINION, TRANSFER
    }

    public enum Priority {
        STAT, URGENT, ROUTINE, SCHEDULED
    }

    public enum Status {
        PENDING, ACCEPTED, REJECTED, MODIFIED, COMPLETED, CANCELLED
    }
}