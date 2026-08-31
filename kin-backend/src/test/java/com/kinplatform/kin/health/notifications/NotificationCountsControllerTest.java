package com.kinplatform.kin.health.notifications;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test del endpoint unificado de contadores de notificaciones (MockMvc).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationCountsControllerTest {

    private static final UUID USER = UUID.randomUUID();
    private static final String EMAIL = "paciente@kin.com";

    @Mock
    private NotificationCountsService notificationCountsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(EMAIL);
        lenient().when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(USER)
                        .email(EMAIL)
                        .role(UserRole.PATIENT)
                        .build()));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new NotificationCountsController(notificationCountsService, userRepository))
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    @Test
    void counts_deberiaDevolverContadoresDelUsuario() throws Exception {
        when(notificationCountsService.countsFor(USER)).thenReturn(new NotificationCounts(3, 2, 1, 2, 0, 4, 1, 5));

        mockMvc.perform(get("/health/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations").value(3))
                .andExpect(jsonPath("$.unreadMessages").value(2))
                .andExpect(jsonPath("$.pendingAppointments").value(1))
                .andExpect(jsonPath("$.upcomingAppointments").value(2))
                .andExpect(jsonPath("$.highUrgencyAlerts").value(0))
                .andExpect(jsonPath("$.pendingTasks").value(4))
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andExpect(jsonPath("$.documents").value(5));
    }

    @Test
    void counts_sinNovedades_deberiaDevolverCeros() throws Exception {
        when(notificationCountsService.countsFor(USER)).thenReturn(NotificationCounts.empty());

        mockMvc.perform(get("/health/notifications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations").value(0))
                .andExpect(jsonPath("$.unreadMessages").value(0))
                .andExpect(jsonPath("$.pendingAppointments").value(0))
                .andExpect(jsonPath("$.upcomingAppointments").value(0))
                .andExpect(jsonPath("$.highUrgencyAlerts").value(0));
    }
}
