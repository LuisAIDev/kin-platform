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
@Table(name = "encounters",
        indexes = {
            @Index(name = "idx_encounters_patient_id", columnList = "patient_id"),
            @Index(name = "idx_encounters_physician_id", columnList = "physician_id"),
            @Index(name = "idx_encounters_organization_id", columnList = "organization_id"),
            @Index(name = "idx_encounters_status", columnList = "status"),
            @Index(name = "idx_encounters_started_at", columnList = "started_at")
        })
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "encounter_type", nullable = false, length = 30)
    private EncounterType encounterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EncounterStatus status = EncounterStatus.IN_PROGRESS;

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum EncounterType {
        OUTPATIENT, INPATIENT, EMERGENCY, TELEMEDICINE, HOME_CARE, DAY_SURGERY
    }

    public enum EncounterStatus {
        SCHEDULED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED, NO_SHOW
    }
}