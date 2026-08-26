package com.kinplatform.kin.health.physician.port;

import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de persistencia de asignaciones médico-paciente (ADR-031).
 *
 * <p>Consulta los pacientes asignados a un médico y permite crear asignaciones
 * (MVP: administrador). La infraestructura lo implementa con JPA (tabla
 * {@code physician_patient_assignments}).</p>
 */
public interface PhysicianPatientRepository {

    List<UUID> findPatientIdsByPhysician(UUID physicianId);

    List<UUID> findPhysicianIdsByPatient(UUID patientId);

    /** Todos los pacientes asignados (unión, sin duplicados) — para métricas del piloto. */
    List<UUID> findAllPatientIds();

    boolean isAssigned(UUID physicianId, UUID patientId);

    PhysicianPatientAssignment assign(PhysicianPatientAssignment assignment);

    void unassign(UUID physicianId, UUID patientId);
}
