package com.kinplatform.kin.health.physician;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria de los puertos del portal de médicos para tests
 * (ADR-031 + ciclo de vida V30). La relación se identifica por la clave natural
 * (physicianId, patientId); {@code assign} inserta o reemplaza (upsert).
 */
public class InMemoryPhysicianRepositories {

    private final Set<PhysicianPatientAssignment> assignments =
            ConcurrentHashMap.newKeySet();
    private final List<ClinicalAlert> alerts = new ArrayList<>();

    public PhysicianPatientRepository patientRepository() {
        return new PhysicianPatientRepository() {
            @Override
            public List<UUID> findPatientIdsByPhysician(UUID physicianId) {
                return assignments.stream()
                        .filter(a -> a.physicianId().equals(physicianId) && a.isActive())
                        .map(PhysicianPatientAssignment::patientId)
                        .toList();
            }

            @Override
            public List<UUID> findPhysicianIdsByPatient(UUID patientId) {
                return assignments.stream()
                        .filter(a -> a.patientId().equals(patientId) && a.isActive())
                        .map(PhysicianPatientAssignment::physicianId)
                        .toList();
            }

            @Override
            public List<UUID> findAllPatientIds() {
                return assignments.stream()
                        .filter(PhysicianPatientAssignment::isActive)
                        .map(PhysicianPatientAssignment::patientId)
                        .distinct()
                        .toList();
            }

            @Override
            public List<PhysicianPatientAssignment> findByPhysicianIdAndStatus(
                    UUID physicianId, RelationshipStatus status) {
                return assignments.stream()
                        .filter(a -> a.physicianId().equals(physicianId) && a.status() == status)
                        .toList();
            }

            @Override
            public List<PhysicianPatientAssignment> findByPatientIdAndStatus(
                    UUID patientId, RelationshipStatus status) {
                return assignments.stream()
                        .filter(a -> a.patientId().equals(patientId) && a.status() == status)
                        .toList();
            }

            @Override
            public Optional<PhysicianPatientAssignment> findByPhysicianIdAndPatientId(
                    UUID physicianId, UUID patientId) {
                return assignments.stream()
                        .filter(a -> a.physicianId().equals(physicianId) && a.patientId().equals(patientId))
                        .findFirst();
            }

            @Override
            public Optional<PhysicianPatientAssignment> findPendingInvitation(UUID physicianId, UUID patientId) {
                return findByPhysicianIdAndPatientId(physicianId, patientId).filter(PhysicianPatientAssignment::isPending);
            }

            @Override
            public boolean isAssigned(UUID physicianId, UUID patientId) {
                return assignments.stream()
                        .anyMatch(a -> a.physicianId().equals(physicianId)
                                && a.patientId().equals(patientId)
                                && a.isActive());
            }

            @Override
            public boolean existsByPhysicianIdAndPatientIdAndStatus(
                    UUID physicianId, UUID patientId, RelationshipStatus status) {
                return assignments.stream()
                        .anyMatch(a -> a.physicianId().equals(physicianId)
                                && a.patientId().equals(patientId)
                                && a.status() == status);
            }

            @Override
            public PhysicianPatientAssignment assign(PhysicianPatientAssignment assignment) {
                assignments.removeIf(a ->
                        a.physicianId().equals(assignment.physicianId())
                                && a.patientId().equals(assignment.patientId()));
                assignments.add(assignment);
                return assignment;
            }

            @Override
            public void unassign(UUID physicianId, UUID patientId) {
                assignments.removeIf(a ->
                        a.physicianId().equals(physicianId) && a.patientId().equals(patientId));
            }
        };
    }

    public ClinicalAlertRepository alertRepository() {
        return new ClinicalAlertRepository() {
            @Override
            public ClinicalAlert save(ClinicalAlert alert) {
                alerts.removeIf(a -> a.id().equals(alert.id()));
                alerts.add(alert);
                return alert;
            }

            @Override
            public Optional<ClinicalAlert> findByIdAndPhysician(UUID id, UUID physicianId) {
                return alerts.stream()
                        .filter(a -> a.id().equals(id) && a.physicianId().equals(physicianId))
                        .findFirst();
            }

            @Override
            public List<ClinicalAlert> findActiveByPhysician(UUID physicianId) {
                return alerts.stream()
                        .filter(a -> a.physicianId().equals(physicianId) && a.isActive())
                        .toList();
            }

            @Override
            public long countActiveHighUrgencyByPhysician(UUID physicianId) {
                return alerts.stream()
                        .filter(a -> a.physicianId().equals(physicianId)
                                && a.isActive()
                                && a.severity() == ClinicalAlert.AlertSeverity.ALTA)
                        .count();
            }
        };
    }

    public static PhysicianPatientAssignment assignment(UUID physicianId, UUID patientId) {
        return PhysicianPatientAssignment.of(physicianId, patientId, OffsetDateTime.now());
    }

    public static PhysicianPatientAssignment pendingAssignment(UUID physicianId, UUID patientId) {
        return PhysicianPatientAssignment.invitation(physicianId, patientId, physicianId, OffsetDateTime.now());
    }
}
