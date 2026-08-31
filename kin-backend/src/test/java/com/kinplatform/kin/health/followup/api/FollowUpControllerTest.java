package com.kinplatform.kin.health.followup.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.kin.health.followup.domain.FollowUpFrequency;
import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.domain.FollowUpPlanWithTasks;
import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import com.kinplatform.kin.health.followup.domain.PatientEvolution;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de los endpoints REST del seguimiento de pacientes (ADR-033) con MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FollowUpControllerTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final String PHYSICIAN_EMAIL = "medico@kin.com";
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private FollowUpService followUpService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(PHYSICIAN_EMAIL);
        lenient().when(userRepository.findByEmail(PHYSICIAN_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .email(PHYSICIAN_EMAIL)
                        .role(UserRole.PHYSICIAN)
                        .build()));
        lenient().when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .role(UserRole.PATIENT)
                        .build()));
        mockMvc = MockMvcBuilders.standaloneSetup(new FollowUpController(followUpService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    @Test
    void createPlan_deberiaCrearPlan() throws Exception {
        when(followUpService.createPlan(eq(PHYSICIAN), eq(PATIENT), eq("Plan"), eq("Desc"), eq(FollowUpFrequency.WEEKLY),
                        any(), any()))
                .thenReturn(FollowUpPlan.of(
                        UUID.randomUUID(), PHYSICIAN, PATIENT, "Plan", "Desc",
                        OffsetDateTime.now(), null, FollowUpFrequency.WEEKLY,
                        com.kinplatform.kin.health.followup.domain.FollowUpStatus.ACTIVE, OffsetDateTime.now()));

        mockMvc.perform(post("/health/followup/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", PATIENT.toString(),
                                "title", "Plan",
                                "description", "Desc",
                                "frequency", "WEEKLY",
                                "startDate", OffsetDateTime.now().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Plan"));
    }

    @Test
    void createPlan_conRolPaciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(post("/health/followup/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", PATIENT.toString(),
                                "title", "Plan",
                                "frequency", "WEEKLY",
                                "startDate", OffsetDateTime.now().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addTask_deberiaAñadirTarea() throws Exception {
        UUID planId = UUID.randomUUID();
        when(followUpService.addTask(eq(PHYSICIAN), eq(planId), eq("Tomar medicación"), any()))
                .thenReturn(FollowUpTask.pending(UUID.randomUUID(), planId, "Tomar medicación",
                        OffsetDateTime.now().plusDays(1)));

        mockMvc.perform(post("/health/followup/plans/" + planId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Tomar medicación",
                                "dueDate", OffsetDateTime.now().plusDays(1).toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Tomar medicación"));
    }

    @Test
    void plansForPatient_deberiaDevolverPlanesConTareas() throws Exception {
        UUID planId = UUID.randomUUID();
        FollowUpPlan plan = FollowUpPlan.of(
                planId, PHYSICIAN, PATIENT, "Plan", "", OffsetDateTime.now(), null,
                FollowUpFrequency.WEEKLY, com.kinplatform.kin.health.followup.domain.FollowUpStatus.ACTIVE,
                OffsetDateTime.now());
        when(followUpService.listPlansForPatient(PHYSICIAN, PATIENT))
                .thenReturn(List.of(new FollowUpPlanWithTasks(plan, List.of(
                        FollowUpTask.pending(UUID.randomUUID(), planId, "Tarea", OffsetDateTime.now())))));

        mockMvc.perform(get("/health/followup/patients/" + PATIENT + "/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Plan"))
                .andExpect(jsonPath("$[0].tasks[0].description").value("Tarea"));
    }

    @Test
    void recordEvolution_deberiaRegistrar() throws Exception {
        when(followUpService.recordEvolution(eq(PHYSICIAN), eq(PATIENT), eq("Mejoría"), any(), eq(true), any()))
                .thenReturn(PatientEvolution.of(
                        UUID.randomUUID(), PATIENT, PHYSICIAN, OffsetDateTime.now(),
                        "Mejoría", Map.of("presion", "120/80"), true, "", OffsetDateTime.now()));

        mockMvc.perform(post("/health/followup/patients/" + PATIENT + "/evolution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "symptoms", "Mejoría",
                                "vitals", Map.of("presion", "120/80"),
                                "medicationAdherence", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symptoms").value("Mejoría"));
    }

    @Test
    void evolutionHistory_deberiaDevolverHistorial() throws Exception {
        when(followUpService.getPatientEvolution(PHYSICIAN, PATIENT))
                .thenReturn(List.of(PatientEvolution.of(
                        UUID.randomUUID(), PATIENT, PHYSICIAN, OffsetDateTime.now(),
                        "E1", Map.of(), true, "", OffsetDateTime.now())));

        mockMvc.perform(get("/health/followup/patients/" + PATIENT + "/evolution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symptoms").value("E1"));
    }

    @Test
    void overdueTasks_deberiaDevolverVencidas() throws Exception {
        when(followUpService.listOverdueTasks(PHYSICIAN))
                .thenReturn(List.of(FollowUpTask.pending(
                        UUID.randomUUID(), UUID.randomUUID(), "Vencida", OffsetDateTime.now().minusDays(1))));

        mockMvc.perform(get("/health/followup/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Vencida"));
    }

    @Test
    void activePlans_deberiaDevolverPlanesDelPaciente() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        when(followUpService.listActivePlansForPatient(PATIENT))
                .thenReturn(List.of(new FollowUpPlanWithTasks(
                        FollowUpPlan.of(
                                UUID.randomUUID(), PHYSICIAN, PATIENT, "Plan activo", "", OffsetDateTime.now(), null,
                                FollowUpFrequency.WEEKLY,
                                com.kinplatform.kin.health.followup.domain.FollowUpStatus.ACTIVE, OffsetDateTime.now()),
                        List.of())));

        mockMvc.perform(get("/health/followup/plans/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Plan activo"));
    }

    @Test
    void completeTask_deberiaCompletar() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        UUID taskId = UUID.randomUUID();
        when(followUpService.completeTask(taskId, PATIENT))
                .thenReturn(FollowUpTask.pending(taskId, UUID.randomUUID(), "Tarea", OffsetDateTime.now())
                        .completed(OffsetDateTime.now()));

        mockMvc.perform(post("/health/followup/tasks/" + taskId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
