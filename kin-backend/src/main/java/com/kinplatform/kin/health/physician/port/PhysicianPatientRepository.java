package com.kinplatform.kin.health.physician.port;

import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de relaciones médico-paciente (ADR-031, ciclo de
 * vida V30).
 *
 * <p>Consulta pacientes asignados a un médico, invitaciones pendientes de un
 * paciente y permite crear/actualizar relaciones. La infraestructura lo
 * implementa con JPA (tabla {@code physician_patient_assignments}).</p>
 */
public interface PhysicianPatientRepository {

    /** Pacientes con relación ACTIVA de un médico (cartera actual). */
    List<UUID> findPatientIdsByPhysician(UUID physicianId);

    /** Médicos con relación ACTIVA de un paciente. */
    List<UUID> findPhysicianIdsByPatient(UUID patientId);

    /** Todos los pacientes con relación ACTIVA (unión, sin duplicados) — para métricas del piloto. */
    List<UUID> findAllPatientIds();

    /** Relaciones de un médico filtrando por estado. */
    List<PhysicianPatientAssignment> findByPhysicianIdAndStatus(UUID physicianId, RelationshipStatus status);

    /** Relaciones de un paciente filtrando por estado (p. ej. invitaciones PENDING). */
    List<PhysicianPatientAssignment> findByPatientIdAndStatus(UUID patientId, RelationshipStatus status);

    /** Busca una relación concreta (si existe). */
    Optional<PhysicianPatientAssignment> findByPhysicianIdAndPatientId(UUID physicianId, UUID patientId);

    /** Invitación pendiente entre un médico y un paciente concreto. */
    Optional<PhysicianPatientAssignment> findPendingInvitation(UUID physicianId, UUID patientId);

    /** La relación está ACTIVA (único estado que habilita acceso clínico, mensajería y citas). */
    boolean isAssigned(UUID physicianId, UUID patientId);

    /** Existe una relación con ese estado (p. ej. evitar duplicar invitaciones ACTIVE/PENDING). */
    boolean existsByPhysicianIdAndPatientIdAndStatus(UUID physicianId, UUID patientId, RelationshipStatus status);

    /** Guarda (inserta o actualiza por clave compuesta) una relación. */
    PhysicianPatientAssignment assign(PhysicianPatientAssignment assignment);

    void unassign(UUID physicianId, UUID patientId);
}
