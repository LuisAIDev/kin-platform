package com.kinplatform.kin.health.followup.adapter;

import com.kinplatform.kin.health.followup.domain.FollowUpFrequency;
import com.kinplatform.kin.health.followup.domain.FollowUpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de plan de seguimiento (tabla {@code follow_up_plans}, ADR-033).
 */
@Entity
@Table(name = "follow_up_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpPlanEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private FollowUpFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FollowUpStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
