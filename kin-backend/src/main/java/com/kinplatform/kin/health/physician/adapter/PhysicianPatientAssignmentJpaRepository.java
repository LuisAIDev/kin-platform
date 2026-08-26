package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.adapter.PhysicianPatientAssignmentEntity.AssignmentId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de asignaciones médico-paciente (ADR-031).
 */
public interface PhysicianPatientAssignmentJpaRepository
        extends JpaRepository<PhysicianPatientAssignmentEntity, AssignmentId> {

    List<PhysicianPatientAssignmentEntity> findByIdPhysicianId(UUID physicianId);

    List<PhysicianPatientAssignmentEntity> findByIdPatientId(UUID patientId);

    boolean existsByIdPhysicianIdAndIdPatientId(UUID physicianId, UUID patientId);
}
