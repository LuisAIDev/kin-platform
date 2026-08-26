package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.adapter.PhysicianPatientAssignmentEntity.AssignmentId;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link PhysicianPatientRepository} (ADR-031).
 *
 * <p>Consulta las asignaciones desde la tabla
 * {@code physician_patient_assignments} (clave compuesta médico-paciente).</p>
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
        return repository.findByIdPhysicianId(physicianId).stream()
                .map(e -> e.getId().getPatientId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPhysicianIdsByPatient(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return repository.findByIdPatientId(patientId).stream()
                .map(e -> e.getId().getPhysicianId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAllPatientIds() {
        return repository.findAll().stream()
                .map(e -> e.getId().getPatientId())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAssigned(UUID physicianId, UUID patientId) {
        return repository.existsByIdPhysicianIdAndIdPatientId(physicianId, patientId);
    }

    @Override
    @Transactional
    public PhysicianPatientAssignment assign(PhysicianPatientAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("assignment no puede ser null");
        }
        var entity = new PhysicianPatientAssignmentEntity();
        entity.setId(new AssignmentId(assignment.physicianId(), assignment.patientId()));
        entity.setAssignedAt(assignment.assignedAt());
        repository.save(entity);
        return assignment;
    }

    @Override
    @Transactional
    public void unassign(UUID physicianId, UUID patientId) {
        repository.deleteById(new AssignmentId(physicianId, patientId));
    }
}
