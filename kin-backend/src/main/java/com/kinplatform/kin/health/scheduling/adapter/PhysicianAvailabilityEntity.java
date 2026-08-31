package com.kinplatform.kin.health.scheduling.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de disponibilidad del médico (tabla {@code physician_availabilities},
 * ADR-034).
 */
@Entity
@Table(name = "physician_availabilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicianAvailabilityEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
