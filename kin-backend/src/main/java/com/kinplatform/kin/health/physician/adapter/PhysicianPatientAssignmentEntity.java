package com.kinplatform.kin.health.physician.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
 * Entidad JPA de asignación médico-paciente (tabla
 * {@code physician_patient_assignments}, ADR-031). Clave compuesta
 * (physicianId, patientId).
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
