package com.kinplatform.kin.health.physician;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación en memoria de los puertos del portal de médicos para tests
 * (ADR-031).
 */
public class InMemoryPhysicianRepositories {

    private final Set<PhysicianPatientAssignment> assignments = new HashSet<>();
    private final List<ClinicalAlert> alerts = new ArrayList<>();

    public PhysicianPatientRepository patientRepository() {
        return new PhysicianPatientRepository() {
            @Override
            public List<UUID> findPatientIdsByPhysician(UUID physicianId) {
                return assignments.stream()
                        .filter(a -> a.physicianId().equals(physicianId))
                        .map(PhysicianPatientAssignment::patientId)
                        .toList();
            }

            @Override
            public List<UUID> findPhysicianIdsByPatient(UUID patientId) {
                return assignments.stream()
                        .filter(a -> a.patientId().equals(patientId))
                        .map(PhysicianPatientAssignment::physicianId)
                        .toList();
            }

            @Override
            public List<UUID> findAllPatientIds() {
                return assignments.stream()
                        .map(PhysicianPatientAssignment::patientId)
                        .distinct()
                        .toList();
            }

            @Override
            public boolean isAssigned(UUID physicianId, UUID patientId) {
                return assignments.stream()
                        .anyMatch(a -> a.physicianId().equals(physicianId)
                                && a.patientId().equals(patientId));
            }

            @Override
            public PhysicianPatientAssignment assign(PhysicianPatientAssignment assignment) {
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
        };
    }

    public static PhysicianPatientAssignment assignment(UUID physicianId, UUID patientId) {
        return PhysicianPatientAssignment.of(physicianId, patientId, OffsetDateTime.now());
    }
}
