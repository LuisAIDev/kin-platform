package com.kinplatform.kin.health.differential.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.differential.domain.DifferentialItem;
import com.kinplatform.kin.health.differential.domain.DifferentialResult;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
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
 * Test del endpoint REST de diagnóstico diferencial con MockMvc (ADR-029).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DifferentialControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "paciente@kin.com";

    @Mock
    private DifferentialService differentialService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var user =
                User.builder().id(USER_ID).email(EMAIL).role(UserRole.PATIENT).build();
        lenient().when(authentication.getName()).thenReturn(EMAIL);
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        mockMvc = MockMvcBuilders.standaloneSetup(new DifferentialController(differentialService, userRepository))
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    private static DifferentialResult result() {
        return new DifferentialResult(
                List.of(new DifferentialItem(
                        UUID.randomUUID(),
                        "Gripe",
                        "Infección viral",
                        0.62,
                        Severity.MODERADO,
                        Urgency.MEDIA,
                        List.of("fiebre"),
                        List.of(RiskFactor.of(UUID.randomUUID(), UUID.randomUUID(), "fumador", 0.4, "Tabaquismo")),
                        List.of(RecommendedTest.of(
                                UUID.randomUUID(), UUID.randomUUID(), "PCR respiratoria", "Detección")),
                        "Gripe se sugiere por la coincidencia de 1 síntoma: fiebre.")),
                List.of("fiebre"),
                0.62,
                "ok",
                "DifferentialEngine",
                "v1");
    }

    @Test
    void getPorConsultation_deberiaDevolverDiagnosticoDiferencial() throws Exception {
        UUID consultationId = UUID.randomUUID();
        when(differentialService.fromConsultation(eq(USER_ID), eq(consultationId), any()))
                .thenReturn(result());

        mockMvc.perform(get("/health/differential").param("consultationId", consultationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].condition").value("Gripe"))
                .andExpect(jsonPath("$.items[0].riskFactors[0].factor").value("fumador"))
                .andExpect(jsonPath("$.items[0].recommendedTests[0].test").value("PCR respiratoria"))
                .andExpect(jsonPath("$.items[0].reasoning").isNotEmpty())
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void postConSintomas_deberiaDevolverDiagnosticoDiferencial() throws Exception {
        when(differentialService.fromSymptoms(eq(List.of("fiebre", "tos")), any()))
                .thenReturn(result());

        mockMvc.perform(post("/health/differential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DifferentialRequest(List.of("fiebre", "tos"), List.of("fumador")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].condition").value("Gripe"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void postSinSintomas_deberiaRechazarCon400() throws Exception {
        mockMvc.perform(post("/health/differential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DifferentialRequest(List.of(), List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSinCoincidencias_deberiaDevolverNoMatch() throws Exception {
        UUID consultationId = UUID.randomUUID();
        when(differentialService.fromConsultation(eq(USER_ID), eq(consultationId), any()))
                .thenReturn(DifferentialResult.empty());

        mockMvc.perform(get("/health/differential").param("consultationId", consultationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }
}

