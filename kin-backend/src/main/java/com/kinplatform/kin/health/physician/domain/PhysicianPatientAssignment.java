package com.kinplatform.kin.health.physician.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Relación médico-paciente (ADR-031, ampliado por el ciclo de vida V30).
 *
 * <p>Entidad de dominio inmutable: relación entre un médico
 * ({@code physicianId}) y un paciente ({@code patientId}) con un estado de
 * ciclo de vida ({@link RelationshipStatus}). Las transiciones producen una
 * nueva instancia inmutable ({@code accepted}, {@code ended}, {@code suspend},
 * {@code reactivate}). El aislamiento estricto del portal garantiza que un
 * médico solo vea pacientes con relación activa.</p>
 *
 * <p>Compatibilidad: {@link #of(UUID, UUID, OffsetDateTime)} crea una relación
 * {@code ACTIVE} directa (asignación por ADMIN/piloto, comportamiento previo
 * intacto); {@link #invitation(UUID, UUID, UUID, OffsetDateTime)} crea una
 * invitación {@code PENDING}.</p>
 */
public record PhysicianPatientAssignment(
        UUID physicianId,
        UUID patientId,
        OffsetDateTime assignedAt,
        RelationshipStatus status,
        UUID invitedBy,
        OffsetDateTime invitedAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime endedAt,
        String endedReason) {

    public PhysicianPatientAssignment {
        if (physicianId == null) {
            throw new IllegalArgumentException("physicianId no puede ser null");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId no puede ser null");
        }
        assignedAt = assignedAt == null ? OffsetDateTime.now() : assignedAt;
        status = status == null ? RelationshipStatus.ACTIVE : status;
    }

    /** Asignación directa ACTIVE (ADMIN / piloto), comportamiento previo intacto. */
    public static PhysicianPatientAssignment of(UUID physicianId, UUID patientId, OffsetDateTime assignedAt) {
        return new PhysicianPatientAssignment(
                physicianId, patientId, assignedAt, RelationshipStatus.ACTIVE, null, null, null, null, null);
    }

    /** Invitación del médico al paciente (estado PENDING). */
    public static PhysicianPatientAssignment invitation(
            UUID physicianId, UUID patientId, UUID invitedBy, OffsetDateTime invitedAt) {
        OffsetDateTime now = invitedAt == null ? OffsetDateTime.now() : invitedAt;
        return new PhysicianPatientAssignment(
                physicianId, patientId, now, RelationshipStatus.PENDING, invitedBy, now, null, null, null);
    }

    /**
     * Invitación a una cuenta existente que aún no tiene capacidad de paciente
     * (falta aceptar el consentimiento de datos de salud). Estado
     * {@code PENDING_CONSENT}: al aceptarlo, la relación pasa directo a ACTIVE.
     */
    public static PhysicianPatientAssignment pendingConsent(
            UUID physicianId, UUID patientId, UUID invitedBy, OffsetDateTime invitedAt) {
        OffsetDateTime now = invitedAt == null ? OffsetDateTime.now() : invitedAt;
        return new PhysicianPatientAssignment(
                physicianId, patientId, now, RelationshipStatus.PENDING_CONSENT, invitedBy, now, null, null, null);
    }

    /** La relación está activa (único estado que habilita comunicación y acceso clínico). */
    public boolean isActive() {
        return status == RelationshipStatus.ACTIVE;
    }

    /** La relación está pendiente de aceptación (invitación estándar). */
    public boolean isPending() {
        return status == RelationshipStatus.PENDING;
    }

    /** La relación está pendiente de que el paciente acepte el consentimiento de salud. */
    public boolean isPendingConsent() {
        return status == RelationshipStatus.PENDING_CONSENT;
    }

    /** La relación está esperando una acción del paciente (PENDING o PENDING_CONSENT). */
    public boolean isAwaitingAcceptance() {
        return status == RelationshipStatus.PENDING || status == RelationshipStatus.PENDING_CONSENT;
    }

    /** Transición a ACTIVE al aceptar el paciente. */
    public PhysicianPatientAssignment accepted(OffsetDateTime acceptedAt) {
        OffsetDateTime at = acceptedAt == null ? OffsetDateTime.now() : acceptedAt;
        return new PhysicianPatientAssignment(
                physicianId, patientId, assignedAt, RelationshipStatus.ACTIVE, invitedBy, invitedAt, at, null, null);
    }

    /** Transición a ENDED con motivo (rechazo, baja, etc.). */
    public PhysicianPatientAssignment ended(OffsetDateTime endedAt, String reason) {
        OffsetDateTime at = endedAt == null ? OffsetDateTime.now() : endedAt;
        return new PhysicianPatientAssignment(
                physicianId, patientId, assignedAt, RelationshipStatus.ENDED, invitedBy, invitedAt, acceptedAt, at, reason);
    }

    /** Transición a SUSPENDED (temporal). */
    public PhysicianPatientAssignment suspended(OffsetDateTime at) {
        OffsetDateTime now = at == null ? OffsetDateTime.now() : at;
        return new PhysicianPatientAssignment(
                physicianId, patientId, assignedAt, RelationshipStatus.SUSPENDED, invitedBy, invitedAt, acceptedAt, null, null);
    }

    /** Transición de vuelta a ACTIVE desde SUSPENDED/ENDED. */
    public PhysicianPatientAssignment reactivated(OffsetDateTime at) {
        OffsetDateTime now = at == null ? OffsetDateTime.now() : at;
        return new PhysicianPatientAssignment(
                physicianId, patientId, assignedAt, RelationshipStatus.ACTIVE, invitedBy, invitedAt, acceptedAt, null, null);
    }

    public static RelationshipStatus statusOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return RelationshipStatus.ACTIVE;
        }
        for (RelationshipStatus s : RelationshipStatus.values()) {
            if (s.name().equalsIgnoreCase(raw.trim())) {
                return s;
            }
        }
        return RelationshipStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhysicianPatientAssignment that)) {
            return false;
        }
        return Objects.equals(physicianId, that.physicianId) && Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicianId, patientId);
    }
}
