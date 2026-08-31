package com.kinplatform.kin.health.automation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.kin.health.automation.domain.ActionType;
import com.kinplatform.kin.health.automation.domain.AutomationRule;
import com.kinplatform.kin.health.automation.domain.TriggerEvent;
import com.kinplatform.kin.health.automation.service.AutomationService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
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
 * Tests HTTP (MockMvc) de RBAC y ownership del m\u00f3dulo de automatizaciones
 * (P1-4/P1-2/P1-3/P1-6) sobre la ruta real {@code /health/automation/**}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutomationControllerTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID OTHER_PHYSICIAN = UUID.randomUUID();
    private static final String PHYSICIAN_EMAIL = "medico@kin.com";
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private AutomationService automationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(PHYSICIAN_EMAIL);
        lenient().when(userRepository.findByEmail(PHYSICIAN_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN).email(PHYSICIAN_EMAIL).role(UserRole.PHYSICIAN).build()));
        lenient().when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(UUID.randomUUID()).email(PATIENT_EMAIL).role(UserRole.PATIENT).build()));
        mockMvc = MockMvcBuilders.standaloneSetup(new AutomationController(automationService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    private AutomationRule rule() {
        return AutomationRule.of(
                "Regla", "desc", TriggerEvent.TRIAGE_PERFORMED,
                "{\"field\":\"urgency\",\"operator\":\"EQ\",\"value\":\"HIGH\"}",
                ActionType.SEND_EMAIL, "{}", true, PHYSICIAN);
    }

    private String createBody() {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "name", "Regla",
                    "description", "desc",
                    "triggerEvent", "TRIAGE_PERFORMED",
                    "conditions", "{\"field\":\"urgency\",\"operator\":\"EQ\",\"value\":\"HIGH\"}",
                    "action", "SEND_EMAIL",
                    "actionParams", "{}",
                    "enabled", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- P1-4 / P1-6: RBAC sobre /health/automation/rules ----------

    @Test
    void createRule_paciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(post("/health/automation/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRule_medico_deberia200() throws Exception {
        when(automationService.createRule(any(), eq("Regla"), eq("desc"), eq(TriggerEvent.TRIAGE_PERFORMED),
                any(), eq(ActionType.SEND_EMAIL), eq("{}"))).thenReturn(rule());

        mockMvc.perform(post("/health/automation/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isOk());
    }

    // ---------- P1-2: listado ----------

    @Test
    void listRules_medicoConPhysicianIdAjeno_deberiaUsarIdentidadAutenticada() throws Exception {
        when(automationService.listRules(any(), eq(OTHER_PHYSICIAN))).thenReturn(List.of(rule()));

        mockMvc.perform(get("/health/automation/rules")
                        .param("physicianId", OTHER_PHYSICIAN.toString()))
                .andExpect(status().isOk());

        verify(automationService).listRules(any(), eq(OTHER_PHYSICIAN));
    }

    @Test
    void listRules_paciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(get("/health/automation/rules"))
                .andExpect(status().isForbidden());

        verify(automationService, never()).listRules(any(), any());
    }

    // ---------- P1-3: update/toggle/delete requieren PHYSICIAN/ADMIN ----------

    @Test
    void updateRule_paciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/health/automation/rules/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleRule_paciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/health/automation/rules/" + UUID.randomUUID() + "/toggle")
                        .param("enabled", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRule_paciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/health/automation/rules/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
