package com.kinplatform.kin.health.dashboard.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.dashboard.domain.CarePlan;
import com.kinplatform.kin.health.dashboard.domain.HealthSummary;
import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test del endpoint REST del dashboard de salud con MockMvc (ADR-030).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "paciente@kin.com";

    @Mock
    private DashboardService dashboardService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        var user =
                User.builder().id(USER_ID).email(EMAIL).role(UserRole.PATIENT).build();
        lenient().when(authentication.getName()).thenReturn(EMAIL);
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboardService, userRepository))
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    private static TriageConsultation consultation() {
        return TriageConsultation.of(
                UUID.randomUUID(),
                USER_ID,
                List.of("fiebre"),
                List.of(new TriageConditionResult(
                        UUID.randomUUID(),
                        "Gripe",
                        "D",
                        0.8,
                        Severity.MODERADO,
                        Urgency.MEDIA,
                        "R",
                        List.of("fiebre"))),
                OffsetDateTime.now());
    }

    @Test
    void summary_deberiaDevolverResumen() throws Exception {
        when(dashboardService.summary(USER_ID))
                .thenReturn(new HealthSummary(
                        3, 1, List.of(new HealthSummary.FrequentCondition("Gripe", 2)), OffsetDateTime.now(), 1));

        mockMvc.perform(get("/health/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConsultations").value(3))
                .andExpect(jsonPath("$.totalDifferentials").value(1))
                .andExpect(jsonPath("$.topConditions[0].name").value("Gripe"))
                .andExpect(jsonPath("$.topConditions[0].occurrences").value(2))
                .andExpect(jsonPath("$.activeReminders").value(1));
    }

    @Test
    void history_deberiaDevolverPagina() throws Exception {
        var page = new PageImpl<>(List.of(consultation()), PageRequest.of(0, 10), 1);
        when(dashboardService.history(eq(USER_ID), any())).thenReturn(page);

        mockMvc.perform(get("/health/dashboard/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symptoms[0]").value("fiebre"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    void historyPorId_deberiaDevolverDetalle() throws Exception {
        var c = consultation();
        when(dashboardService.consultationDetail(USER_ID, c.id())).thenReturn(c);

        mockMvc.perform(get("/health/dashboard/history/" + c.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.id().toString()))
                .andExpect(jsonPath("$.results[0].condition").value("Gripe"));
    }

    @Test
    void profile_deberiaDevolverPerfilVacio() throws Exception {
        when(dashboardService.profile(USER_ID)).thenReturn(PatientProfile.empty(USER_ID));

        mockMvc.perform(get("/health/dashboard/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.riskFactors").isEmpty());
    }

    @Test
    void updateProfile_deberiaGuardar() throws Exception {
        when(dashboardService.updateProfile(eq(USER_ID), eq(List.of("fumador")), eq(List.of("hipertensión"))))
                .thenReturn(
                        PatientProfile.of(USER_ID, List.of("fumador"), List.of("hipertensión"), OffsetDateTime.now()));

        mockMvc.perform(put("/health/dashboard/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatientProfileRequest(List.of("fumador"), List.of("hipertensión")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskFactors[0]").value("fumador"))
                .andExpect(jsonPath("$.chronicConditions[0]").value("hipertensión"));
    }

    @Test
    void carePlan_deberiaDevolverRecomendaciones() throws Exception {
        when(dashboardService.carePlan(USER_ID))
                .thenReturn(new CarePlan(
                        List.of("Controla tu glucosa."),
                        List.of("diabetes"),
                        List.of(new CarePlan.CareRecommendation("diabetes", "Controla tu glucosa.", "ALTA"))));

        mockMvc.perform(get("/health/dashboard/care-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0]").value("Controla tu glucosa."))
                .andExpect(jsonPath("$.detailed[0].priority").value("ALTA"));
    }

    @Test
    void createReminder_deberiaCrearCon201() throws Exception {
        var reminder = Reminder.of(
                UUID.randomUUID(),
                USER_ID,
                Reminder.ReminderType.CITA,
                "Consulta",
                OffsetDateTime.now().plusDays(7),
                true,
                OffsetDateTime.now());
        when(dashboardService.createReminder(eq(USER_ID), any())).thenReturn(reminder);

        mockMvc.perform(post("/health/dashboard/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderRequest(
                                "CITA", "Consulta", OffsetDateTime.now().plusDays(7)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CITA"));
    }

    @Test
    void createReminder_conTipoInvalido_deberiaRechazarCon400() throws Exception {
        mockMvc.perform(post("/health/dashboard/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderRequest(
                                "INVALIDO", "Consulta", OffsetDateTime.now().plusDays(7)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reminders_deberiaDevolverLista() throws Exception {
        when(dashboardService.reminders(USER_ID))
                .thenReturn(List.of(Reminder.of(
                        UUID.randomUUID(),
                        USER_ID,
                        Reminder.ReminderType.MEDICACION,
                        "Toma metformina",
                        OffsetDateTime.now(),
                        true,
                        OffsetDateTime.now())));

        mockMvc.perform(get("/health/dashboard/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Toma metformina"))
                .andExpect(jsonPath("$[0].type").value("MEDICACION"));
    }
}
