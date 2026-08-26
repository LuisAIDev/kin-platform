package com.kinplatform.kin.health.dashboard.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de recordatorio del paciente (tabla {@code reminders}, ADR-030).
 */
@Entity
@Table(name = "reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private com.kinplatform.kin.health.dashboard.domain.Reminder.ReminderType type;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (active == null) {
            active = true;
        }
    }
}
