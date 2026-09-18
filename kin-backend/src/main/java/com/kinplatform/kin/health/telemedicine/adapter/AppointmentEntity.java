package com.kinplatform.kin.health.telemedicine.adapter;

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
 * Entidad JPA de cita de telemedicina (tabla {@code appointments}, ADR-032).
 */
@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "rescheduled_from")
    private UUID rescheduledFrom;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "availability_slot_id")
    private UUID availabilitySlotId;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "video_room_id", length = 100)
    private String videoRoomId;

    @Column(name = "video_room_created_at")
    private OffsetDateTime videoRoomCreatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus.PENDIENTE;
        }
    }
}
