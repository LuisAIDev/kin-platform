package com.kinplatform.kin.health.physician.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Asignación médico-paciente (ADR-031).
 *
 * <p>Entidad de dominio inmutable: relación entre un médico ({@code physicianId})
 * y un paciente ({@code patientId}). El aislamiento estricto del portal
 * garantiza que un médico solo vea pacientes asignados a él.</p>
 */
public record PhysicianPatientAssignment(UUID physicianId, UUID patientId, OffsetDateTime assignedAt) {

    public PhysicianPatientAssignment {
        if (physicianId == null) {
            throw new IllegalArgumentException("physicianId no puede ser null");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId no puede ser null");
        }
        assignedAt = assignedAt == null ? OffsetDateTime.now() : assignedAt;
    }

    public static PhysicianPatientAssignment of(UUID physicianId, UUID patientId, OffsetDateTime assignedAt) {
        return new PhysicianPatientAssignment(physicianId, patientId, assignedAt);
    }
}
