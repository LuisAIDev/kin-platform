package com.kinplatform.kin.health.followup;

import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.domain.FollowUpStatus;
import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus;
import com.kinplatform.kin.health.followup.domain.PatientEvolution;
import com.kinplatform.kin.health.followup.port.FollowUpPlanRepository;
import com.kinplatform.kin.health.followup.port.FollowUpTaskRepository;
import com.kinplatform.kin.health.followup.port.PatientEvolutionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementaciones en memoria de los puertos de seguimiento para tests (ADR-033).
 */
public class InMemoryFollowUpRepositories {

    private final java.util.Map<UUID, FollowUpPlan> plans = new ConcurrentHashMap<>();
    private final java.util.Map<UUID, FollowUpTask> tasks = new ConcurrentHashMap<>();
    private final java.util.Map<UUID, PatientEvolution> evolutions = new ConcurrentHashMap<>();

    public FollowUpPlanRepository planRepository() {
        return new FollowUpPlanRepository() {
            @Override
            public FollowUpPlan save(FollowUpPlan plan) {
                plans.put(plan.id(), plan);
                return plan;
            }

            @Override
            public Optional<FollowUpPlan> findById(UUID id) {
                return Optional.ofNullable(plans.get(id));
            }

            @Override
            public List<FollowUpPlan> findByPatientIdAndPhysicianId(UUID patientId, UUID physicianId) {
                return plans.values().stream()
                        .filter(p -> p.patientId().equals(patientId) && p.physicianId().equals(physicianId))
                        .sorted(Comparator.comparing(FollowUpPlan::createdAt).reversed())
                        .toList();
            }

            @Override
            public List<FollowUpPlan> findActiveByPatientId(UUID patientId) {
                return plans.values().stream()
                        .filter(p -> p.patientId().equals(patientId) && p.isActive())
                        .sorted(Comparator.comparing(FollowUpPlan::createdAt).reversed())
                        .toList();
            }

            @Override
            public List<FollowUpPlan> findByPhysicianId(UUID physicianId) {
                return plans.values().stream()
                        .filter(p -> p.physicianId().equals(physicianId))
                        .toList();
            }

            @Override
            public List<UUID> findDistinctPhysicianIdsWithActivePlans() {
                return plans.values().stream()
                        .filter(FollowUpPlan::isActive)
                        .map(FollowUpPlan::physicianId)
                        .distinct()
                        .toList();
            }
        };
    }

    public FollowUpTaskRepository taskRepository() {
        return new FollowUpTaskRepository() {
            @Override
            public FollowUpTask save(FollowUpTask task) {
                tasks.put(task.id(), task);
                return task;
            }

            @Override
            public Optional<FollowUpTask> findById(UUID id) {
                return Optional.ofNullable(tasks.get(id));
            }

            @Override
            public List<FollowUpTask> findByPlanId(UUID planId) {
                return tasks.values().stream()
                        .filter(t -> t.planId().equals(planId))
                        .sorted(Comparator.comparing(FollowUpTask::dueDate))
                        .toList();
            }

            @Override
            public List<FollowUpTask> findByPlanIdIn(Collection<UUID> planIds) {
                return tasks.values().stream()
                        .filter(t -> planIds.contains(t.planId()))
                        .sorted(Comparator.comparing(FollowUpTask::dueDate))
                        .toList();
            }

            @Override
            public List<FollowUpTask> findPendingRemindableInWindow(OffsetDateTime from, OffsetDateTime to) {
                return tasks.values().stream()
                        .filter(t -> t.isPending() && !t.reminderSent())
                        .filter(t -> !t.dueDate().isBefore(from) && !t.dueDate().isAfter(to))
                        .toList();
            }

            @Override
            public List<FollowUpTask> findPendingDueBefore(OffsetDateTime now) {
                return tasks.values().stream()
                        .filter(t -> t.isPending() && t.dueDate().isBefore(now))
                        .toList();
            }

            @Override
            public List<FollowUpTask> findOpenDueBefore(Collection<UUID> planIds, OffsetDateTime now) {
                return tasks.values().stream()
                        .filter(t -> planIds.contains(t.planId()))
                        .filter(t -> t.status() == FollowUpTaskStatus.PENDING
                                || t.status() == FollowUpTaskStatus.OVERDUE)
                        .filter(t -> t.dueDate().isBefore(now))
                        .sorted(Comparator.comparing(FollowUpTask::dueDate))
                        .toList();
            }

            @Override
            public long countPendingByPlanIdIn(Collection<UUID> planIds) {
                return tasks.values().stream()
                        .filter(t -> planIds.contains(t.planId()) && t.isPending())
                        .count();
            }
        };
    }

    public PatientEvolutionRepository evolutionRepository() {
        return new PatientEvolutionRepository() {
            @Override
            public PatientEvolution save(PatientEvolution evolution) {
                evolutions.put(evolution.id(), evolution);
                return evolution;
            }

            @Override
            public List<PatientEvolution> findByPatientIdAndPhysicianId(UUID patientId, UUID physicianId) {
                return evolutions.values().stream()
                        .filter(e -> e.patientId().equals(patientId) && e.physicianId().equals(physicianId))
                        .sorted(Comparator.comparing(PatientEvolution::recordedAt).reversed())
                        .toList();
            }

            @Override
            public List<PatientEvolution> findByPatientId(UUID patientId) {
                return evolutions.values().stream()
                        .filter(e -> e.patientId().equals(patientId))
                        .sorted(Comparator.comparing(PatientEvolution::recordedAt).reversed())
                        .toList();
            }

            @Override
            public Optional<PatientEvolution> findLatestByPatientId(UUID patientId) {
                return evolutions.values().stream()
                        .filter(e -> e.patientId().equals(patientId))
                        .max(Comparator.comparing(PatientEvolution::recordedAt));
            }
        };
    }

    /** Helper para construir un plan activo en tests. */
    public static FollowUpPlan plan(UUID id, UUID physicianId, UUID patientId) {
        return FollowUpPlan.of(
                id,
                physicianId,
                patientId,
                "Plan de prueba",
                "Descripción",
                OffsetDateTime.now(),
                null,
                com.kinplatform.kin.health.followup.domain.FollowUpFrequency.WEEKLY,
                FollowUpStatus.ACTIVE,
                OffsetDateTime.now());
    }

    public static FollowUpTask task(UUID id, UUID planId, OffsetDateTime dueDate) {
        return FollowUpTask.pending(id, planId, "Tarea de prueba", dueDate);
    }
}
