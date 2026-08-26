package com.kinplatform.kin.health.pilot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de los endpoints administrativos del piloto (setup + métricas).
 */
@ExtendWith(MockitoExtension.class)
class PilotAdminControllerTest {

    @Mock
    private PilotOnboardingService onboardingService;

    @Mock
    private PilotMetricsService metricsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PilotAdminController(onboardingService, metricsService))
                .build();
    }

    @Test
    void setup_deberiaCrearGrupoPiloto() throws Exception {
        when(onboardingService.setup(anyList(), anyList(), any(), anyList()))
                .thenReturn(new PilotOnboardingService.PilotSetupResult(12, 10));

        mockMvc.perform(post("/admin/health/pilot/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PilotAdminController.PilotSetupRequest(
                                List.of("p1@kin.com", "p2@kin.com"),
                                List.of("m1@kin.com"),
                                "password123",
                                List.of(new PilotAdminController.AssignmentRequest("p1@kin.com", "m1@kin.com"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCreated").value(12))
                .andExpect(jsonPath("$.assignmentsCreated").value(10));
    }

    @Test
    void setup_sinPacientes_deberiaRechazarCon400() throws Exception {
        mockMvc.perform(post("/admin/health/pilot/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PilotAdminController.PilotSetupRequest(
                                List.of(), List.of("m1@kin.com"), "password123", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void metrics_deberiaDevolverInformeAnonimizado() throws Exception {
        when(metricsService.report()).thenReturn(new PilotMetricsService.PilotMetrics(10, 8, 0.8, 15, 40, 6, 45.0, 2));

        mockMvc.perform(get("/admin/health/pilot/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(10))
                .andExpect(jsonPath("$.patientsWithTriage").value(8))
                .andExpect(jsonPath("$.triageCompletionRate").value(0.8))
                .andExpect(jsonPath("$.avgPhysicianResponseMinutes").value(45.0));
    }
}
