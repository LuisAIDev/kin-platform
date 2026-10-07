package com.kinplatform.kin.health.scheduling.api;

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
import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Test de los endpoints REST de agenda y disponibilidad (ADR-034) con MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchedulingControllerTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final String PHYSICIAN_EMAIL = "medico@kin.com";
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private SchedulingService schedulingService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new SchedulingController(schedulingService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    private static PhysicianAvailability availability() {
        return PhysicianAvailability.of(
                UUID.randomUUID(), PHYSICIAN, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 30, true,
                OffsetDateTime.now());
    }

    private static Appointment appointment() {
        return Appointment.of(
                UUID.randomUUID(), PATIENT, PHYSICIAN, OffsetDateTime.now().plusDays(1), "motivo",
                AppointmentStatus.PENDIENTE, OffsetDateTime.now());
    }

    @Test
    void setAvailability_deberiaGuardarHorario() throws Exception {
        when(schedulingService.setAvailability(eq(PHYSICIAN), eq(DayOfWeek.MONDAY), any(), any(), eq(30), eq(true)))
                .thenReturn(availability());

        mockMvc.perform(post("/health/scheduling/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dayOfWeek", "MONDAY",
                                "startTime", "09:00",
                                "endTime", "11:00",
                                "slotDurationMinutes", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"));
    }

    @Test
    void getAvailability_conRolPaciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(get("/health/scheduling/availability"))
                .andExpect(status().isForbidden());
    }

    @Test
    void slotsForPhysician_deberiaDevolverSlots() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        when(schedulingService.getAvailableSlotsForPatient(eq(PATIENT), eq(PHYSICIAN), any()))
                .thenReturn(List.of(OffsetDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)));

        mockMvc.perform(get("/health/scheduling/physicians/" + PHYSICIAN + "/slots")
                        .param("date", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    void requestAppointment_deberiaCrearCita() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        when(schedulingService.requestAppointment(eq(PATIENT), eq(PHYSICIAN), any(), eq(30), eq("motivo")))
                .thenReturn(appointment());

        mockMvc.perform(post("/health/scheduling/appointments/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "physicianId", PHYSICIAN.toString(),
                                "scheduledAt", OffsetDateTime.now().plusDays(1).toString(),
                                "durationMinutes", 30,
                                "reason", "motivo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"));
    }

    @Test
    void confirmAppointment_deberiaConfirmar() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulingService.confirmAppointment(id, PHYSICIAN))
                .thenReturn(appointment().confirmed());

        mockMvc.perform(put("/health/scheduling/appointments/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    void cancelAppointment_deberiaCancelar() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        UUID id = UUID.randomUUID();
        when(schedulingService.cancelAppointment(id, PATIENT, "motivo"))
                .thenReturn(appointment().canceled("motivo"));

        mockMvc.perform(put("/health/scheduling/appointments/" + id + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "motivo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void upcomingAppointments_deberiaDevolverLista() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        when(schedulingService.upcomingAppointments(PATIENT, false)).thenReturn(List.of(appointment()));

        mockMvc.perform(get("/health/scheduling/appointments/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDIENTE"));
    }
}

