package com.kinplatform.kin.health.followup.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.followup.domain.FollowUpFrequency;
import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.domain.FollowUpPlanWithTasks;
import com.kinplatform.kin.health.followup.domain.FollowUpStatus;
import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus;
import com.kinplatform.kin.health.followup.domain.PatientEvolution;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del seguimiento de pacientes (ADR-033).
 *
 * <ul>
 *   <li><strong>Médico</strong>: crear plan, añadir tareas, listar planes de un
 *       paciente, registrar/ver evolución y listar tareas vencidas.</li>
 *   <li><strong>Paciente</strong>: ver sus planes activos, completar tareas y
 *       ver su historial de evolución (solo lectura).</li>
 * </ul>
 *
 * <p>Permisos: las operaciones sobre datos de un paciente exigen relación
 * {@code ACTIVE} (validada en el servicio con {@code RelationshipAccessValidator});
 * el paciente opera solo sobre sus propios datos (userId del JWT).</p>
 */
@RestController
@RequestMapping({
    "/health/followup",
    "/medical/followup"
})
public class FollowUpController {

    private static final Logger log = LoggerFactory.getLogger(FollowUpController.class);

    private final FollowUpService followUpService;
    private final UserRepository userRepository;

    public FollowUpController(FollowUpService followUpService, UserRepository userRepository) {
        this.followUpService = followUpService;
        this.userRepository = userRepository;
    }

    // ---------- Médico: planes ----------

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> createPlan(
            Authentication authentication, @Valid @RequestBody CreatePlanRequest request) {
        User user = requirePhysician(authentication);
        FollowUpPlan plan = followUpService.createPlan(
                user.getId(),
                request.patientId(),
                request.title(),
                request.description(),
                request.frequency(),
                request.startDate(),
                request.endDate());
        log.info("=== FOLLOWUP CREATE PLAN === physician={}, patient={}", user.getId(), request.patientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanResponse.from(plan, List.of()));
    }

    @PostMapping("/plans/{planId}/tasks")
    public ResponseEntity<TaskResponse> addTask(
            Authentication authentication,
            @PathVariable UUID planId,
            @Valid @RequestBody AddTaskRequest request) {
        User user = requirePhysician(authentication);
        FollowUpTask task = followUpService.addTask(user.getId(), planId, request.description(), request.dueDate());
        log.info("=== FOLLOWUP ADD TASK === plan={}, physician={}", planId, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @GetMapping("/patients/{patientId}/plans")
    public ResponseEntity<List<PlanResponse>> plansForPatient(
            Authentication authentication, @PathVariable UUID patientId) {
        User user = requirePhysician(authentication);
        List<PlanResponse> plans = followUpService.listPlansForPatient(user.getId(), patientId).stream()
                .map(FollowUpController::toPlanResponse)
                .toList();
        return ResponseEntity.ok(plans);
    }

    // ---------- Médico: evolución ----------

    @PostMapping("/patients/{patientId}/evolution")
    public ResponseEntity<EvolutionResponse> recordEvolution(
            Authentication authentication,
            @PathVariable UUID patientId,
            @Valid @RequestBody RecordEvolutionRequest request) {
        User user = requirePhysician(authentication);
        PatientEvolution evolution = followUpService.recordEvolution(
                user.getId(),
                patientId,
                request.symptoms(),
                request.vitals(),
                request.medicationAdherence(),
                request.notes());
        log.info("=== FOLLOWUP EVOLUTION === patient={}, physician={}", patientId, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(EvolutionResponse.from(evolution));
    }

    @GetMapping("/patients/{patientId}/evolution")
    public ResponseEntity<List<EvolutionResponse>> evolutionHistory(
            Authentication authentication, @PathVariable UUID patientId) {
        User user = requirePhysician(authentication);
        List<EvolutionResponse> history =
                followUpService.getPatientEvolution(user.getId(), patientId).stream()
                        .map(EvolutionResponse::from)
                        .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/tasks/overdue")
    public ResponseEntity<List<TaskResponse>> overdueTasks(Authentication authentication) {
        User user = requirePhysician(authentication);
        List<TaskResponse> tasks = followUpService.listOverdueTasks(user.getId()).stream()
                .map(TaskResponse::from)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    // ---------- Paciente ----------

    @GetMapping("/plans/active")
    public ResponseEntity<List<PlanResponse>> activePlans(Authentication authentication) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        List<PlanResponse> plans = followUpService.listActivePlansForPatient(patientId).stream()
                .map(FollowUpController::toPlanResponse)
                .toList();
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<TaskResponse> completeTask(Authentication authentication, @PathVariable UUID taskId) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        FollowUpTask completed = followUpService.completeTask(taskId, userId);
        log.info("=== FOLLOWUP COMPLETE TASK === task={}, user={}", taskId, userId);
        return ResponseEntity.ok(TaskResponse.from(completed));
    }

    @GetMapping("/evolution")
    public ResponseEntity<List<EvolutionResponse>> ownEvolution(Authentication authentication) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        List<EvolutionResponse> history = followUpService.getOwnEvolution(patientId).stream()
                .map(EvolutionResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    // ---------- Helpers ----------

    private User requirePhysician(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (user.getRole() != UserRole.PHYSICIAN && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Se requiere rol de médico para esta operación");
        }
        return user;
    }

    private static PlanResponse toPlanResponse(FollowUpPlanWithTasks planWithTasks) {
        return PlanResponse.from(planWithTasks.plan(), planWithTasks.tasks());
    }

    public record CreatePlanRequest(
            @NotNull(message = "patientId es obligatorio") UUID patientId,
            @NotBlank(message = "title es obligatorio") String title,
            String description,
            @NotNull(message = "frequency es obligatoria") FollowUpFrequency frequency,
            @NotNull(message = "startDate es obligatoria") OffsetDateTime startDate,
            OffsetDateTime endDate) {}

    public record AddTaskRequest(
            @NotBlank(message = "description es obligatoria") String description,
            @NotNull(message = "dueDate es obligatoria") OffsetDateTime dueDate) {}

    public record RecordEvolutionRequest(
            String symptoms,
            Map<String, Object> vitals,
            Boolean medicationAdherence,
            String notes) {}

    public record PlanResponse(
            UUID planId,
            UUID patientId,
            UUID physicianId,
            String title,
            String description,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            FollowUpFrequency frequency,
            FollowUpStatus status,
            OffsetDateTime createdAt,
            List<TaskResponse> tasks) {
        static PlanResponse from(FollowUpPlan plan, List<FollowUpTask> tasks) {
            return new PlanResponse(
                    plan.id(),
                    plan.patientId(),
                    plan.physicianId(),
                    plan.title(),
                    plan.description(),
                    plan.startDate(),
                    plan.endDate(),
                    plan.frequency(),
                    plan.status(),
                    plan.createdAt(),
                    tasks.stream().map(TaskResponse::from).toList());
        }
    }

    public record TaskResponse(
            UUID taskId,
            UUID planId,
            String description,
            OffsetDateTime dueDate,
            FollowUpTaskStatus status,
            OffsetDateTime completedAt) {
        static TaskResponse from(FollowUpTask task) {
            return new TaskResponse(
                    task.id(),
                    task.planId(),
                    task.description(),
                    task.dueDate(),
                    task.status(),
                    task.completedAt());
        }
    }

    public record EvolutionResponse(
            UUID id,
            UUID patientId,
            UUID physicianId,
            OffsetDateTime recordedAt,
            String symptoms,
            Map<String, Object> vitals,
            Boolean medicationAdherence,
            String notes) {
        static EvolutionResponse from(PatientEvolution evolution) {
            return new EvolutionResponse(
                    evolution.id(),
                    evolution.patientId(),
                    evolution.physicianId(),
                    evolution.recordedAt(),
                    evolution.symptoms(),
                    evolution.vitals(),
                    evolution.medicationAdherence(),
                    evolution.notes());
        }
    }
}

