package com.kinplatform.kin.health.followup.api;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.followup.config.FollowUpProperties;
import com.kinplatform.kin.health.followup.domain.FollowUpFrequency;
import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.domain.FollowUpPlanWithTasks;
import com.kinplatform.kin.health.followup.domain.FollowUpStatus;
import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import com.kinplatform.kin.health.followup.domain.PatientEvolution;
import com.kinplatform.kin.health.followup.event.FollowUpPlanCreatedEvent;
import com.kinplatform.kin.health.followup.event.FollowUpTaskAddedEvent;
import com.kinplatform.kin.health.followup.event.FollowUpTaskCompletedEvent;
import com.kinplatform.kin.health.followup.event.PatientEvolutionRecordedEvent;
import com.kinplatform.kin.health.followup.port.FollowUpPlanRepository;
import com.kinplatform.kin.health.followup.port.FollowUpTaskRepository;
import com.kinplatform.kin.health.followup.port.PatientEvolutionRepository;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del seguimiento de pacientes (ADR-033).
 *
 * <p>Permite al médico crear planes de seguimiento y tareas, registrar la
 * evolución del paciente, y al paciente ver sus planes y completar tareas.
 * Todo acceso a datos del paciente exige relación {@code ACTIVE} (Área 5,
 * {@link RelationshipAccessValidator}). Los eventos se publican de forma
 * transaccional vía outbox (fallback al bus en memoria).</p>
 */
@Service
public class FollowUpService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);

    private final FollowUpPlanRepository planRepository;
    private final FollowUpTaskRepository taskRepository;
    private final PatientEvolutionRepository evolutionRepository;
    private final RelationshipAccessValidator accessValidator;
    private final FollowUpProperties properties;
    private final DomainEventBus eventBus;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditService auditService;

    public FollowUpService(
            FollowUpPlanRepository planRepository,
            FollowUpTaskRepository taskRepository,
            PatientEvolutionRepository evolutionRepository,
            RelationshipAccessValidator accessValidator,
            FollowUpProperties properties,
            AuditService auditService) {
        this(planRepository, taskRepository, evolutionRepository, accessValidator, properties, auditService, null, null);
    }

    @Autowired
    public FollowUpService(
            FollowUpPlanRepository planRepository,
            FollowUpTaskRepository taskRepository,
            PatientEvolutionRepository evolutionRepository,
            RelationshipAccessValidator accessValidator,
            FollowUpProperties properties,
            AuditService auditService,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.evolutionRepository = evolutionRepository;
        this.accessValidator = accessValidator;
        this.properties = properties;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    // ---------- Planes ----------

    @Transactional
    public FollowUpPlan createPlan(
            UUID physicianId,
            UUID patientId,
            String title,
            String description,
            FollowUpFrequency frequency,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);

        FollowUpPlan plan = FollowUpPlan.of(
                UUID.randomUUID(),
                physicianId,
                patientId,
                title,
                description,
                startDate,
                endDate,
                frequency,
                FollowUpStatus.ACTIVE,
                OffsetDateTime.now());
        planRepository.save(plan);

        for (FollowUpTask task : initialTasks(plan)) {
            taskRepository.save(task);
        }
        publish(new FollowUpPlanCreatedEvent(plan.id(), patientId, physicianId));
        auditService.logAccess(physicianId, AuditAction.CREATE_PLAN, AuditResourceType.PLAN_SEGUIMIENTO, plan.id(),
                patientId, java.util.Map.of());
        log.info("FollowUpService: plan creado {} para paciente {} por médico {}", plan.id(), patientId, physicianId);
        return plan;
    }

    @Transactional
    public FollowUpTask addTask(UUID physicianId, UUID planId, String description, OffsetDateTime dueDate) {
        requireEnabled();
        FollowUpPlan plan = requirePlan(planId);
        if (!plan.physicianId().equals(physicianId)) {
            throw new FollowUpAccessDeniedException("Solo el médico del plan puede añadir tareas");
        }
        accessValidator.requireActiveRelationship(physicianId, plan.patientId());

        FollowUpTask task = FollowUpTask.pending(UUID.randomUUID(), planId, description, dueDate);
        taskRepository.save(task);
        publish(new FollowUpTaskAddedEvent(task.id(), planId, plan.patientId(), physicianId));
        auditService.logAccess(physicianId, AuditAction.ADD_TASK, AuditResourceType.TAREA, task.id(),
                plan.patientId(), java.util.Map.of("planId", planId));
        log.info("FollowUpService: tarea añadida {} al plan {}", task.id(), planId);
        return task;
    }

    @Transactional
    public FollowUpTask completeTask(UUID taskId, UUID userId) {
        requireEnabled();
        FollowUpTask task = requireTask(taskId);
        FollowUpPlan plan = requirePlan(task.planId());
        if (!plan.involves(userId)) {
            throw new FollowUpAccessDeniedException("El usuario no participa en este plan de seguimiento");
        }
        if (userId.equals(plan.physicianId())) {
            accessValidator.requireActiveRelationship(plan.physicianId(), plan.patientId());
        }
        FollowUpTask completed = task.completed(OffsetDateTime.now());
        taskRepository.save(completed);
        publish(new FollowUpTaskCompletedEvent(task.id(), plan.id(), plan.patientId(), plan.physicianId(), userId));
        auditService.logAccess(userId, AuditAction.COMPLETE_TASK, AuditResourceType.TAREA, task.id(),
                plan.patientId(), java.util.Map.of("planId", plan.id()));
        log.info("FollowUpService: tarea {} completada por {}", task.id(), userId);
        return completed;
    }

    // ---------- Evolución ----------

    @Transactional
    public PatientEvolution recordEvolution(
            UUID physicianId,
            UUID patientId,
            String symptoms,
            Map<String, Object> vitals,
            Boolean medicationAdherence,
            String notes) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);

        PatientEvolution evolution = PatientEvolution.of(
                UUID.randomUUID(),
                patientId,
                physicianId,
                OffsetDateTime.now(),
                symptoms,
                vitals,
                medicationAdherence,
                notes,
                OffsetDateTime.now());
        evolutionRepository.save(evolution);
        publish(new PatientEvolutionRecordedEvent(evolution.id(), patientId, physicianId));
        auditService.logAccess(physicianId, AuditAction.RECORD_EVOLUTION, AuditResourceType.EVOLUCION, evolution.id(),
                patientId, java.util.Map.of());
        log.info("FollowUpService: evolución registrada para paciente {} por médico {}", patientId, physicianId);
        return evolution;
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public List<FollowUpPlanWithTasks> listPlansForPatient(UUID physicianId, UUID patientId) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);
        return withTasks(planRepository.findByPatientIdAndPhysicianId(patientId, physicianId));
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlanWithTasks> listActivePlansForPatient(UUID patientId) {
        requireEnabled();
        if (patientId == null) {
            return List.of();
        }
        return withTasks(planRepository.findActiveByPatientId(patientId));
    }

    @Transactional(readOnly = true)
    public List<FollowUpTask> listOverdueTasks(UUID physicianId) {
        requireEnabled();
        List<UUID> planIds = planRepository.findByPhysicianId(physicianId).stream()
                .map(FollowUpPlan::id)
                .toList();
        return taskRepository.findOpenDueBefore(planIds, OffsetDateTime.now());
    }

    @Transactional
    public List<PatientEvolution> getPatientEvolution(UUID physicianId, UUID patientId) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);
        auditService.logAccess(physicianId, AuditAction.VIEW_EVOLUTION, AuditResourceType.EVOLUCION, null, patientId,
                java.util.Map.of());
        return evolutionRepository.findByPatientIdAndPhysicianId(patientId, physicianId);
    }

    @Transactional(readOnly = true)
    public List<PatientEvolution> getOwnEvolution(UUID patientId) {
        requireEnabled();
        if (patientId == null) {
            return List.of();
        }
        return evolutionRepository.findByPatientId(patientId);
    }

    /** Tareas pendientes del paciente (para el contador de notificaciones). */
    @Transactional(readOnly = true)
    public long pendingTaskCountForPatient(UUID patientId) {
        if (!properties.isEnabled() || patientId == null) {
            return 0;
        }
        List<UUID> planIds = planRepository.findActiveByPatientId(patientId).stream()
                .map(FollowUpPlan::id)
                .toList();
        return taskRepository.countPendingByPlanIdIn(planIds);
    }

    // ---------- Internos ----------

    private List<FollowUpPlanWithTasks> withTasks(List<FollowUpPlan> plans) {
        return plans.stream()
                .map(p -> new FollowUpPlanWithTasks(p, taskRepository.findByPlanId(p.id())))
                .toList();
    }

    /** Tareas iniciales según la frecuencia del plan. */
    static List<FollowUpTask> initialTasks(FollowUpPlan plan) {
        OffsetDateTime start = plan.startDate();
        List<FollowUpTask> tasks = new ArrayList<>();
        switch (plan.frequency()) {
            case DAILY -> {
                for (int i = 0; i < 7; i++) {
                    tasks.add(FollowUpTask.pending(
                            UUID.randomUUID(), plan.id(), "Seguimiento diario (día " + (i + 1) + ")",
                            start.plusDays(i)));
                }
            }
            case WEEKLY -> {
                for (int i = 0; i < 4; i++) {
                    tasks.add(FollowUpTask.pending(
                            UUID.randomUUID(), plan.id(), "Revisión semanal (semana " + (i + 1) + ")",
                            start.plusWeeks(i)));
                }
            }
            case MONTHLY -> {
                for (int i = 0; i < 3; i++) {
                    tasks.add(FollowUpTask.pending(
                            UUID.randomUUID(), plan.id(), "Revisión mensual (mes " + (i + 1) + ")",
                            start.plusMonths(i)));
                }
            }
        }
        return tasks;
    }

    private FollowUpPlan requirePlan(UUID planId) {
        return planRepository
                .findById(planId)
                .orElseThrow(() -> new FollowUpNotFoundException("Plan de seguimiento no encontrado: " + planId));
    }

    private FollowUpTask requireTask(UUID taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> new FollowUpNotFoundException("Tarea de seguimiento no encontrada: " + taskId));
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new FollowUpDisabledException();
        }
    }

    private void publish(DomainEvent event) {
        if (outboxEventPublisher != null) {
            outboxEventPublisher.publish(event);
        } else if (eventBus != null) {
            eventBus.publish(event);
        }
    }
}
