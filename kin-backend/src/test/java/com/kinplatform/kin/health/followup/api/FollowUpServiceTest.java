package com.kinplatform.kin.health.followup.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.health.followup.InMemoryFollowUpRepositories;
import com.kinplatform.kin.health.followup.config.FollowUpProperties;
import com.kinplatform.kin.health.followup.domain.FollowUpFrequency;
import com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus;
import com.kinplatform.kin.health.followup.event.FollowUpPlanCreatedEvent;
import com.kinplatform.kin.health.followup.event.FollowUpTaskAddedEvent;
import com.kinplatform.kin.health.followup.event.FollowUpTaskCompletedEvent;
import com.kinplatform.kin.health.followup.event.PatientEvolutionRecordedEvent;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.physician.access.RelationshipNotActiveException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests del servicio de seguimiento de pacientes (ADR-033): creación de planes,
 * tareas, evolución, validación de permisos por relación ACTIVE y eventos.
 */
class FollowUpServiceTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    private InMemoryDomainEventBus bus;
    private FollowUpProperties properties;

    @BeforeEach
    void setUp() {
        bus = new InMemoryDomainEventBus();
        properties = new FollowUpProperties();
    }

    private FollowUpService service(InMemoryPhysicianRepositories physicians, InMemoryFollowUpRepositories repos) {
        var auditProps = new com.kinplatform.common.audit.config.AuditProperties();
        auditProps.setEnabled(false);
        return new FollowUpService(
                repos.planRepository(),
                repos.taskRepository(),
                repos.evolutionRepository(),
                new RelationshipAccessValidator(physicians.patientRepository()),
                properties,
                new com.kinplatform.common.audit.api.AuditService(null, null, null, auditProps),
                bus,
                null);
    }

    private InMemoryPhysicianRepositories activePhysicians() {
        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        return physicians;
    }

    // ---------- Planes ----------

    @Test
    void createPlan_conRelacionActiva_deberiaCrearPlanConTareas() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);

        var plan = service.createPlan(PHYSICIAN, PATIENT, "Control de presión", "Medir 2 veces al día",
                FollowUpFrequency.DAILY, OffsetDateTime.now(), null);

        assertEquals(PATIENT, plan.patientId());
        assertEquals(7, repos.taskRepository().findByPlanId(plan.id()).size());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof FollowUpPlanCreatedEvent));
    }

    @Test
    void createPlan_frecuenciaSemanal_deberiaCrearCuatroTareas() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);

        var plan = service.createPlan(PHYSICIAN, PATIENT, "Plan semanal", null,
                FollowUpFrequency.WEEKLY, OffsetDateTime.now(), null);

        assertEquals(4, repos.taskRepository().findByPlanId(plan.id()).size());
    }

    @Test
    void createPlan_sinRelacionActiva_deberiaLanzar() {
        var service = service(new InMemoryPhysicianRepositories(), new InMemoryFollowUpRepositories());

        assertThrows(
                RelationshipNotActiveException.class,
                () -> service.createPlan(PHYSICIAN, PATIENT, "Plan", null, FollowUpFrequency.WEEKLY,
                        OffsetDateTime.now(), null));
    }

    @Test
    void createPlan_conRelacionPendiente_deberiaLanzar() {
        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));
        var service = service(physicians, new InMemoryFollowUpRepositories());

        assertThrows(
                RelationshipNotActiveException.class,
                () -> service.createPlan(PHYSICIAN, PATIENT, "Plan", null, FollowUpFrequency.WEEKLY,
                        OffsetDateTime.now(), null));
    }

    // ---------- Tareas ----------

    @Test
    void addTask_medicoDelPlan_deberiaAñadirTarea() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));

        var task = service.addTask(PHYSICIAN, planId, "Tomar medicación", OffsetDateTime.now().plusDays(1));

        assertEquals("Tomar medicación", task.description());
        assertTrue(task.isPending());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof FollowUpTaskAddedEvent));
    }

    @Test
    void addTask_medicoAjeno_deberiaLanzar() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));

        assertThrows(
                FollowUpAccessDeniedException.class,
                () -> service.addTask(UUID.randomUUID(), planId, "Tarea", OffsetDateTime.now()));
    }

    @Test
    void addTask_conRelacionFinalizada_deberiaLanzar() {
        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(
                InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT)
                        .ended(OffsetDateTime.now(), "REJECTED_BY_PATIENT"));
        var repos = new InMemoryFollowUpRepositories();
        var service = service(physicians, repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));

        assertThrows(
                RelationshipNotActiveException.class,
                () -> service.addTask(PHYSICIAN, planId, "Tarea", OffsetDateTime.now()));
    }

    @Test
    void completeTask_pacienteImplicado_deberiaCompletar() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));
        UUID taskId = UUID.randomUUID();
        repos.taskRepository().save(InMemoryFollowUpRepositories.task(taskId, planId, OffsetDateTime.now()));

        var completed = service.completeTask(taskId, PATIENT);

        assertEquals(FollowUpTaskStatus.COMPLETED, completed.status());
        assertTrue(completed.completedAt() != null);
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof FollowUpTaskCompletedEvent));
    }

    @Test
    void completeTask_usuarioAjeno_deberiaLanzar() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));
        UUID taskId = UUID.randomUUID();
        repos.taskRepository().save(InMemoryFollowUpRepositories.task(taskId, planId, OffsetDateTime.now()));

        assertThrows(FollowUpAccessDeniedException.class, () -> service.completeTask(taskId, UUID.randomUUID()));
    }

    @Test
    void completeTask_tareaInexistente_deberiaLanzar() {
        var service = service(activePhysicians(), new InMemoryFollowUpRepositories());

        assertThrows(FollowUpNotFoundException.class, () -> service.completeTask(UUID.randomUUID(), PATIENT));
    }

    // ---------- Evolución ----------

    @Test
    void recordEvolution_conRelacionActiva_deberiaGuardar() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);

        var evolution = service.recordEvolution(
                PHYSICIAN, PATIENT, "Mejoría", Map.of("presion", "120/80"), true, "Sin novedades");

        assertEquals(PATIENT, evolution.patientId());
        assertEquals("Mejoría", evolution.symptoms());
        assertEquals(1, repos.evolutionRepository().findByPatientId(PATIENT).size());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof PatientEvolutionRecordedEvent));
    }

    @Test
    void recordEvolution_sinRelacion_deberiaLanzar() {
        var service = service(new InMemoryPhysicianRepositories(), new InMemoryFollowUpRepositories());

        assertThrows(
                RelationshipNotActiveException.class,
                () -> service.recordEvolution(PHYSICIAN, PATIENT, "x", Map.of(), null, null));
    }

    @Test
    void getPatientEvolution_conRelacion_deberiaDevolverHistorial() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        service.recordEvolution(PHYSICIAN, PATIENT, "Evolución 1", Map.of("peso", "70"), true, "ok");
        service.recordEvolution(PHYSICIAN, PATIENT, "Evolución 2", Map.of("peso", "69"), true, "ok");

        var history = service.getPatientEvolution(PHYSICIAN, PATIENT);

        assertEquals(2, history.size());
    }

    @Test
    void getPatientEvolution_sinRelacion_deberiaLanzar() {
        var service = service(new InMemoryPhysicianRepositories(), new InMemoryFollowUpRepositories());

        assertThrows(RelationshipNotActiveException.class, () -> service.getPatientEvolution(PHYSICIAN, PATIENT));
    }

    @Test
    void getOwnEvolution_deberiaDevolverHistorialDelPaciente() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        service.recordEvolution(PHYSICIAN, PATIENT, "E1", Map.of(), null, null);

        assertEquals(1, service.getOwnEvolution(PATIENT).size());
    }

    // ---------- Consultas ----------

    @Test
    void listPlansForPatient_deberiaDevolverPlanesConTareas() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));
        repos.taskRepository().save(InMemoryFollowUpRepositories.task(UUID.randomUUID(), planId, OffsetDateTime.now()));

        var plans = service.listPlansForPatient(PHYSICIAN, PATIENT);

        assertEquals(1, plans.size());
        assertEquals(1, plans.get(0).tasks().size());
    }

    @Test
    void listActivePlansForPatient_deberiaDevolverSoloActivos() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(UUID.randomUUID(), PHYSICIAN, PATIENT));
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(UUID.randomUUID(), PHYSICIAN, PATIENT));

        var active = service.listActivePlansForPatient(PATIENT);

        assertEquals(2, active.size());
    }

    @Test
    void listOverdueTasks_deberiaDevolverSoloVencidas() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));
        repos.taskRepository().save(
                InMemoryFollowUpRepositories.task(UUID.randomUUID(), planId, OffsetDateTime.now().minusDays(2)));
        repos.taskRepository().save(
                InMemoryFollowUpRepositories.task(UUID.randomUUID(), planId, OffsetDateTime.now().plusDays(5)));

        var overdue = service.listOverdueTasks(PHYSICIAN);

        assertEquals(1, overdue.size());
    }

    @Test
    void pendingTaskCountForPatient_deberiaContarPendientes() {
        var repos = new InMemoryFollowUpRepositories();
        var service = service(activePhysicians(), repos);
        UUID planId = UUID.randomUUID();
        repos.planRepository().save(InMemoryFollowUpRepositories.plan(planId, PHYSICIAN, PATIENT));
        UUID task1 = UUID.randomUUID();
        repos.taskRepository().save(InMemoryFollowUpRepositories.task(task1, planId, OffsetDateTime.now().plusDays(1)));
        UUID task2 = UUID.randomUUID();
        repos.taskRepository().save(InMemoryFollowUpRepositories.task(task2, planId, OffsetDateTime.now().plusDays(2)));
        // Completar una: queda 1 pendiente.
        service.completeTask(task1, PATIENT);

        assertEquals(1, service.pendingTaskCountForPatient(PATIENT));
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        properties.setEnabled(false);
        var service = service(activePhysicians(), new InMemoryFollowUpRepositories());

        assertThrows(
                FollowUpDisabledException.class,
                () -> service.createPlan(PHYSICIAN, PATIENT, "Plan", null, FollowUpFrequency.WEEKLY,
                        OffsetDateTime.now(), null));
        assertFalse(service.pendingTaskCountForPatient(PATIENT) > 0);
    }
}

