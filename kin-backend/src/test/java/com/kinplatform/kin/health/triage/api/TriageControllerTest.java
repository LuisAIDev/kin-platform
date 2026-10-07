package com.kinplatform.kin.health.triage.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
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
 * Test del endpoint REST de triaje con MockMvc (ADR-028).
 *
 * <p>Se usa {@code standaloneSetup} con el controlador y sus dependencias
 * simuladas: valida el mapeo de request/response y el disclaimer sin levantar
 * la cadena de seguridad completa. Un {@code HandlerMethodArgumentResolver}
 * inyecta el {@link Authentication} simulado en el parámetro del controller.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TriageControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "paciente@kin.com";

    @Mock
    private TriageService triageService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new TriageController(triageService, userRepository))
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    @Test
    void triage_conSintomas_deberiaDevolverResultadosConDisclaimer() throws Exception {
        var condition = new TriageConditionResult(
                UUID.randomUUID(),
                "Gripe",
                "Infección viral",
                0.78,
                Severity.MODERADO,
                Urgency.MEDIA,
                "Consulta médica.",
                List.of("fiebre", "tos"));
        var result = new TriageResult(List.of(condition), List.of(), 0.78, "triaje ok", "TriageEngine", "v1");
        when(triageService.analyze(eq(USER_ID), eq(List.of("fiebre", "tos")))).thenReturn(result);

        mockMvc.perform(post("/health/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TriageRequest(List.of("fiebre", "tos")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.results[0].condition").value("Gripe"))
                .andExpect(jsonPath("$.results[0].probability").value(0.78))
                .andExpect(jsonPath("$.results[0].severity").value("MODERADO"))
                .andExpect(jsonPath("$.results[0].urgency").value("MEDIA"))
                .andExpect(jsonPath("$.results[0].recommendation").value("Consulta médica."))
                .andExpect(jsonPath("$.results[0].matchedSymptoms[0]").value("fiebre"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void triage_sinCoincidencias_deberiaDevolverNoMatchConDisclaimer() throws Exception {
        when(triageService.analyze(eq(USER_ID), any())).thenReturn(TriageResult.empty());

        mockMvc.perform(post("/health/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TriageRequest(List.of("x")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void triage_sinSintomas_deberiaRechazarCon400() throws Exception {
        mockMvc.perform(post("/health/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TriageRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void symptoms_deberiaDevolverElCatalogo() throws Exception {
        var symptom = com.kinplatform.kin.health.triage.domain.Symptom.of(
                UUID.randomUUID(), "fiebre", "Temperatura elevada", "R50.9");
        when(triageService.listSymptoms()).thenReturn(List.of(symptom));

        mockMvc.perform(get("/health/triage/symptoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("fiebre"));
    }

    @Test
    void history_deberiaDevolverSoloLasConsultasDelUsuarioAutenticado() throws Exception {
        var consultation =
                TriageConsultation.of(UUID.randomUUID(), USER_ID, List.of("fiebre"), List.of(), OffsetDateTime.now());
        when(triageService.historyExcludingHidden(USER_ID)).thenReturn(List.of(consultation));

        mockMvc.perform(get("/health/triage/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symptoms[0]").value("fiebre"));
    }
}

