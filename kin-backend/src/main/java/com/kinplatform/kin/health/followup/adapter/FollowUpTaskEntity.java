package com.kinplatform.kin.health.followup.adapter;

import com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de tarea de seguimiento (tabla {@code follow_up_tasks}, ADR-033).
 */
@Entity
@Table(name = "follow_up_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpTaskEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "due_date", nullable = false)
    private OffsetDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FollowUpTaskStatus status;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
