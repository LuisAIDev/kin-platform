package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.adapter.PhysicianPatientAssignmentEntity.AssignmentId;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de relaciones médico-paciente (ADR-031 + V30).
 */
public interface PhysicianPatientAssignmentJpaRepository
        extends JpaRepository<PhysicianPatientAssignmentEntity, AssignmentId> {

    List<PhysicianPatientAssignmentEntity> findByIdPhysicianId(UUID physicianId);

    List<PhysicianPatientAssignmentEntity> findByIdPatientId(UUID patientId);

    List<PhysicianPatientAssignmentEntity> findByIdPhysicianIdAndStatus(
            UUID physicianId, RelationshipStatus status);

    List<PhysicianPatientAssignmentEntity> findByIdPatientIdAndStatus(UUID patientId, RelationshipStatus status);

    Optional<PhysicianPatientAssignmentEntity> findByIdPhysicianIdAndIdPatientId(UUID physicianId, UUID patientId);

    boolean existsByIdPhysicianIdAndIdPatientId(UUID physicianId, UUID patientId);

    boolean existsByIdPhysicianIdAndIdPatientIdAndStatus(
            UUID physicianId, UUID patientId, RelationshipStatus status);
}
