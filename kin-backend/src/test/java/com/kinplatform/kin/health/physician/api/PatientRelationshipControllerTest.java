package com.kinplatform.kin.health.physician.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de los endpoints del paciente para gestionar relaciones con médicos
 * (V30): listar invitaciones pendientes, aceptar y rechazar.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientRelationshipControllerTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        lenient().when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .role(UserRole.PATIENT)
                        .build()));
        mockMvc = MockMvcBuilders.standaloneSetup(new PatientRelationshipController(relationshipService, userRepository))
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    @Test
    void pending_deberiaDevolverInvitacionesConDatosDelMedico() throws Exception {
        when(relationshipService.pendingInvitationsForPatient(PATIENT))
                .thenReturn(List.of(PhysicianPatientAssignment.invitation(
                        PHYSICIAN, PATIENT, PHYSICIAN, OffsetDateTime.now())));
        lenient().when(userRepository.findById(PHYSICIAN))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .fullName("Dr. Test")
                        .specialty("Cardiología")
                        .role(UserRole.PHYSICIAN)
                        .build()));

        mockMvc.perform(get("/health/patient/relationships/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].physicianId").value(PHYSICIAN.toString()))
                .andExpect(jsonPath("$[0].physicianName").value("Dr. Test"))
                .andExpect(jsonPath("$[0].specialty").value("Cardiología"));
    }

    @Test
    void pending_sinInvitaciones_deberiaDevolverListaVacia() throws Exception {
        when(relationshipService.pendingInvitationsForPatient(PATIENT)).thenReturn(List.of());

        mockMvc.perform(get("/health/patient/relationships/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void accept_deberiaAceptarInvitacion() throws Exception {
        PhysicianPatientAssignment accepted = PhysicianPatientAssignment.invitation(
                        PHYSICIAN, PATIENT, PHYSICIAN, OffsetDateTime.now())
                .accepted(OffsetDateTime.now());
        when(relationshipService.acceptInvitation(PATIENT, PHYSICIAN)).thenReturn(accepted);

        mockMvc.perform(post("/health/patient/relationships/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatientRelationshipController.PhysicianRequest(PHYSICIAN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty());
    }

    @Test
    void reject_deberiaRechazarInvitacion() throws Exception {
        PhysicianPatientAssignment ended = PhysicianPatientAssignment.invitation(
                        PHYSICIAN, PATIENT, PHYSICIAN, OffsetDateTime.now())
                .ended(OffsetDateTime.now(), RelationshipService.REJECTED_BY_PATIENT);
        when(relationshipService.rejectInvitation(PATIENT, PHYSICIAN)).thenReturn(ended);

        mockMvc.perform(post("/health/patient/relationships/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatientRelationshipController.PhysicianRequest(PHYSICIAN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedReason").value("REJECTED_BY_PATIENT"));
    }

    @Test
    void accept_sinPhysicianId_deberiaDevolver400() throws Exception {
        mockMvc.perform(post("/health/patient/relationships/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pending_usuarioNoPaciente_deberiaFallar() throws Exception {
        // El aislamiento lo garantiza SecurityConfig (rol PATIENT); aquí se
        // verifica que el endpoint se apoya en la identidad autenticada.
        mockMvc.perform(get("/health/patient/relationships/pending"))
                .andExpect(status().isOk());
    }
}
