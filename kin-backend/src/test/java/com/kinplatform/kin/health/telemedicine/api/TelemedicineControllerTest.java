package com.kinplatform.kin.health.telemedicine.api;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.domain.Message;
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
 * Test del endpoint REST de telemedicina con MockMvc (ADR-032).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelemedicineControllerTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    private static final String EMAIL = "paciente@kin.com";

    @Mock
    private TelemedicineService telemedicineService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        var user = User.builder().id(USER).email(EMAIL).role(UserRole.PATIENT).build();
        lenient().when(authentication.getName()).thenReturn(EMAIL);
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        mockMvc = MockMvcBuilders.standaloneSetup(new TelemedicineController(telemedicineService, userRepository))
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    private static Message message() {
        return Message.of(
                UUID.randomUUID(),
                USER,
                OTHER,
                Message.conversationIdOf(USER, OTHER),
                "hola doctor",
                false,
                OffsetDateTime.now());
    }

    @Test
    void sendMessage_deberiaEnviar() throws Exception {
        when(telemedicineService.sendMessage(eq(USER), eq(OTHER), eq("hola doctor")))
                .thenReturn(message());

        mockMvc.perform(post("/health/telemedicine/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TelemedicineController.SendMessageRequest(OTHER, "hola doctor"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverId").value(OTHER.toString()))
                .andExpect(jsonPath("$.content").value("hola doctor"));
    }

    @Test
    void sendMessage_sinContenido_deberiaRechazarCon400() throws Exception {
        mockMvc.perform(post("/health/telemedicine/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TelemedicineController.SendMessageRequest(OTHER, "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void messages_deberiaDevolverConversacion() throws Exception {
        when(telemedicineService.markConversationRead(USER, OTHER)).thenReturn(List.of(message()));

        mockMvc.perform(get("/health/telemedicine/messages").param("with", OTHER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("hola doctor"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void unread_deberiaDevolverContador() throws Exception {
        when(telemedicineService.unreadCount(USER)).thenReturn(3L);

        mockMvc.perform(get("/health/telemedicine/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(3));
    }

    @Test
    void requestAppointment_deberiaCrear() throws Exception {
        when(telemedicineService.requestAppointment(eq(USER), eq(OTHER), any(), eq("Control")))
                .thenReturn(Appointment.of(
                        UUID.randomUUID(),
                        USER,
                        OTHER,
                        OffsetDateTime.now().plusDays(2),
                        "Control",
                        AppointmentStatus.PENDIENTE,
                        OffsetDateTime.now()));

        mockMvc.perform(post("/health/telemedicine/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelemedicineController.AppointmentRequest(
                                OTHER, OffsetDateTime.now().plusDays(2), "Control"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.reason").value("Control"));
    }

    @Test
    void updateAppointmentStatus_deberiaActualizar() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        when(telemedicineService.updateAppointmentStatus(USER, appointmentId, AppointmentStatus.CONFIRMADA))
                .thenReturn(Appointment.of(
                        appointmentId,
                        OTHER,
                        USER,
                        OffsetDateTime.now().plusDays(2),
                        "Control",
                        AppointmentStatus.CONFIRMADA,
                        OffsetDateTime.now()));

        mockMvc.perform(put("/health/telemedicine/appointments/" + appointmentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TelemedicineController.StatusRequest(AppointmentStatus.CONFIRMADA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    void appointments_deberiaDevolverLista() throws Exception {
        when(telemedicineService.listAppointments(USER, false))
                .thenReturn(List.of(Appointment.of(
                        UUID.randomUUID(),
                        USER,
                        OTHER,
                        OffsetDateTime.now().plusDays(2),
                        "Control",
                        AppointmentStatus.PENDIENTE,
                        OffsetDateTime.now())));

        mockMvc.perform(get("/health/telemedicine/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDIENTE"));
    }
}
