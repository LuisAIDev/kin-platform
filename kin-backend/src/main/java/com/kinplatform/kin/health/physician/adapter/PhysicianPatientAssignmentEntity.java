package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de relación médico-paciente (tabla
 * {@code physician_patient_assignments}, ADR-031 + ciclo de vida V30). Clave
 * compuesta (physicianId, patientId).
 */
@Entity
@Table(name = "physician_patient_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicianPatientAssignmentEntity {

    @EmbeddedId
    private AssignmentId id;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RelationshipStatus status;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "invited_at")
    private OffsetDateTime invitedAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "ended_reason")
    private String endedReason;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentId implements Serializable {

        @Column(name = "physician_id")
        private UUID physicianId;

        @Column(name = "patient_id")
        private UUID patientId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AssignmentId that)) {
                return false;
            }
            return Objects.equals(physicianId, that.physicianId) && Objects.equals(patientId, that.patientId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(physicianId, patientId);
        }
    }
}
