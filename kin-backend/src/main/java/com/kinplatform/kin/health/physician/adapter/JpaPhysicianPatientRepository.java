package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.adapter.PhysicianPatientAssignmentEntity.AssignmentId;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link PhysicianPatientRepository} (ADR-031 + V30).
 *
 * <p>Consulta las relaciones desde la tabla
 * {@code physician_patient_assignments} (clave compuesta médico-paciente) con
 * su estado de ciclo de vida. {@code isAssigned} solo es {@code true} para
 * relaciones {@code ACTIVE}: el acceso clínico, la mensajería y las citas
 * requieren relación activa.</p>
 */
@Component
public class JpaPhysicianPatientRepository implements PhysicianPatientRepository {

    private final PhysicianPatientAssignmentJpaRepository repository;

    public JpaPhysicianPatientRepository(PhysicianPatientAssignmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPatientIdsByPhysician(UUID physicianId) {
        if (physicianId == null) {
            return List.of();
        }
        return repository.findByIdPhysicianIdAndStatus(physicianId, RelationshipStatus.ACTIVE).stream()
                .map(e -> e.getId().getPatientId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPhysicianIdsByPatient(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return repository.findByIdPatientIdAndStatus(patientId, RelationshipStatus.ACTIVE).stream()
                .map(e -> e.getId().getPhysicianId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAllPatientIds() {
        return repository.findAll().stream()
                .filter(e -> e.getStatus() == RelationshipStatus.ACTIVE)
                .map(e -> e.getId().getPatientId())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhysicianPatientAssignment> findByPhysicianIdAndStatus(UUID physicianId, RelationshipStatus status) {
        if (physicianId == null || status == null) {
            return List.of();
        }
        return repository.findByIdPhysicianIdAndStatus(physicianId, status).stream()
                .map(JpaPhysicianPatientRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhysicianPatientAssignment> findByPatientIdAndStatus(UUID patientId, RelationshipStatus status) {
        if (patientId == null || status == null) {
            return List.of();
        }
        return repository.findByIdPatientIdAndStatus(patientId, status).stream()
                .map(JpaPhysicianPatientRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PhysicianPatientAssignment> findByPhysicianIdAndPatientId(UUID physicianId, UUID patientId) {
        if (physicianId == null || patientId == null) {
            return Optional.empty();
        }
        return repository.findByIdPhysicianIdAndIdPatientId(physicianId, patientId).map(
                JpaPhysicianPatientRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PhysicianPatientAssignment> findPendingInvitation(UUID physicianId, UUID patientId) {
        return findByPhysicianIdAndPatientId(physicianId, patientId)
                .filter(PhysicianPatientAssignment::isPending);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAssigned(UUID physicianId, UUID patientId) {
        return repository.existsByIdPhysicianIdAndIdPatientIdAndStatus(
                physicianId, patientId, RelationshipStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhysicianIdAndPatientIdAndStatus(
            UUID physicianId, UUID patientId, RelationshipStatus status) {
        if (physicianId == null || patientId == null || status == null) {
            return false;
        }
        return repository.existsByIdPhysicianIdAndIdPatientIdAndStatus(physicianId, patientId, status);
    }

    @Override
    @Transactional
    public PhysicianPatientAssignment assign(PhysicianPatientAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("assignment no puede ser null");
        }
        repository.save(toEntity(assignment));
        return assignment;
    }

    @Override
    @Transactional
    public void unassign(UUID physicianId, UUID patientId) {
        repository.deleteById(new AssignmentId(physicianId, patientId));
    }

    private static PhysicianPatientAssignmentEntity toEntity(PhysicianPatientAssignment a) {
        PhysicianPatientAssignmentEntity entity = new PhysicianPatientAssignmentEntity();
        entity.setId(new AssignmentId(a.physicianId(), a.patientId()));
        entity.setAssignedAt(a.assignedAt());
        entity.setStatus(a.status());
        entity.setInvitedBy(a.invitedBy());
        entity.setInvitedAt(a.invitedAt());
        entity.setAcceptedAt(a.acceptedAt());
        entity.setEndedAt(a.endedAt());
        entity.setEndedReason(a.endedReason());
        return entity;
    }

    private static PhysicianPatientAssignment toDomain(PhysicianPatientAssignmentEntity e) {
        return new PhysicianPatientAssignment(
                e.getId().getPhysicianId(),
                e.getId().getPatientId(),
                e.getAssignedAt(),
                e.getStatus(),
                e.getInvitedBy(),
                e.getInvitedAt(),
                e.getAcceptedAt(),
                e.getEndedAt(),
                e.getEndedReason());
    }
}
