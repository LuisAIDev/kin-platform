package com.kinplatform.kin.health.physician.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.domain.PatientSummary;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
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
 * Test del endpoint REST del portal de médicos con MockMvc (ADR-031 + V30).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhysicianControllerTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final String EMAIL = "medico@kin.com";

    @Mock
    private PhysicianService physicianService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private PhysicianApplicationService applicationService;

    @BeforeEach
    void setUp() {
        var user = User.builder()
                .id(PHYSICIAN)
                .email(EMAIL)
                .role(UserRole.PHYSICIAN)
                .build();
        lenient().when(authentication.getName()).thenReturn(EMAIL);
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        mockMvc = MockMvcBuilders.standaloneSetup(new PhysicianController(physicianService, relationshipService, userRepository, applicationService))
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    @Test
    void patients_deberiaDevolverListaPaginada() throws Exception {
        when(physicianService.listPatients(eq(PHYSICIAN), any()))
                .thenReturn(new PageImpl<>(
                        List.of(PatientSummary.active(
                                PATIENT,
                                "Paciente Test",
                                List.of("Gripe"),
                                List.of("fumador"),
                                List.of(),
                                3,
                                OffsetDateTime.now(),
                                1)),
                        PageRequest.of(0, 10),
                        1));

        mockMvc.perform(get("/health/physician/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientName").value("Paciente Test"))
                .andExpect(jsonPath("$.content[0].totalTriages").value(3))
                .andExpect(jsonPath("$.content[0].activeAlerts").value(1))
                .andExpect(jsonPath("$.content[0].relationshipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void patients_conStatusPending_deberiaFiltrarPorEstado() throws Exception {
        when(physicianService.listPatients(eq(PHYSICIAN), eq(RelationshipStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(
                        List.of(PatientSummary.pending(PATIENT, "Paciente Test")),
                        PageRequest.of(0, 10),
                        1));

        mockMvc.perform(get("/health/physician/patients").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].relationshipStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].patientName").value("Paciente Test"));
    }

    @Test
    void invite_deberiaInvocarInvitacion() throws Exception {
        UUID invited = UUID.randomUUID();
        PhysicianPatientAssignment invitation = PhysicianPatientAssignment.invitation(
                PHYSICIAN, invited, PHYSICIAN, OffsetDateTime.now());
        when(relationshipService.invitePatient(PHYSICIAN, "paciente@kin.com", "Hola"))
                .thenReturn(invitation);
        when(userRepository.findById(invited))
                .thenReturn(Optional.of(User.builder()
                        .id(invited)
                        .email("paciente@kin.com")
                        .role(UserRole.PATIENT)
                        .build()));

        mockMvc.perform(post("/health/physician/patients/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PhysicianController.InviteRequest("paciente@kin.com", "Hola"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.patientId").value(invited.toString()))
                .andExpect(jsonPath("$.patientEmail").value("paciente@kin.com"));
    }

    @Test
    void patientSummary_deberiaDevolverResumen() throws Exception {
        when(physicianService.patientSummary(PHYSICIAN, PATIENT))
                .thenReturn(PatientSummary.active(
                        PATIENT,
                        "Paciente Test",
                        List.of("Gripe"),
                        List.of("fumador"),
                        List.of("hipertensión"),
                        3,
                        OffsetDateTime.now(),
                        0));

        mockMvc.perform(get("/health/physician/patients/" + PATIENT + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeConditions[0]").value("Gripe"))
                .andExpect(jsonPath("$.riskFactors[0]").value("fumador"))
                .andExpect(jsonPath("$.chronicConditions[0]").value("hipertensión"));
    }

    @Test
    void patientHistory_deberiaDevolverHistorial() throws Exception {
        var consultation = TriageConsultation.of(
                UUID.randomUUID(),
                PATIENT,
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
        when(physicianService.patientHistory(PHYSICIAN, PATIENT)).thenReturn(List.of(consultation));

        mockMvc.perform(get("/health/physician/patients/" + PATIENT + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symptoms[0]").value("fiebre"))
                .andExpect(jsonPath("$[0].results[0].condition").value("Gripe"));
    }

    @Test
    void alerts_deberiaDevolverAlertasActivas() throws Exception {
        when(physicianService.activeAlerts(PHYSICIAN))
                .thenReturn(List.of(ClinicalAlert.of(
                        UUID.randomUUID(),
                        PATIENT,
                        PHYSICIAN,
                        ClinicalAlert.AlertType.HIGH_URGENCY_TRIAGE,
                        ClinicalAlert.AlertSeverity.ALTA,
                        "Triaje de alta urgencia: Angina de pecho",
                        ClinicalAlert.AlertStatus.PENDING,
                        OffsetDateTime.now(),
                        null)));

        mockMvc.perform(get("/health/physician/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("ALTA"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void acknowledge_deberiaMarcarComoAtendida() throws Exception {
        UUID alertId = UUID.randomUUID();
        when(physicianService.acknowledgeAlert(PHYSICIAN, alertId))
                .thenReturn(ClinicalAlert.of(
                        alertId,
                        PATIENT,
                        PHYSICIAN,
                        ClinicalAlert.AlertType.HIGH_URGENCY_TRIAGE,
                        ClinicalAlert.AlertSeverity.ALTA,
                        "Triaje de alta urgencia",
                        ClinicalAlert.AlertStatus.ACKNOWLEDGED,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()));

        mockMvc.perform(post("/health/physician/alerts/" + alertId + "/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }
}
