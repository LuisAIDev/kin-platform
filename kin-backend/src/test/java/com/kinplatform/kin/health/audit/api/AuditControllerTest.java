package com.kinplatform.common.audit.api;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.common.audit.InMemoryAuditLogRepository;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
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
 * Test de los endpoints de auditoría (admin y paciente) con MockMvc (ADR-035).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditControllerTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@kin.com";
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    private InMemoryAuditLogRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc adminMvc;
    private MockMvc patientMvc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAuditLogRepository();
        repository.save(com.kinplatform.common.audit.domain.AuditLog.of(
                UUID.randomUUID(), UUID.randomUUID(), AuditAction.VIEW_HISTORY, AuditResourceType.PACIENTE,
                PATIENT, PATIENT, OffsetDateTime.now(), "1.2.3.4", "test-agent", java.util.Map.of()));

        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        lenient().when(userRepository.findByEmail(ADMIN_EMAIL))
                .thenReturn(Optional.of(User.builder().id(ADMIN).email(ADMIN_EMAIL).role(UserRole.ADMIN).build()));
        lenient().when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder().id(PATIENT).email(PATIENT_EMAIL).role(UserRole.PATIENT).build()));

        adminMvc = build(new AuditAdminController(repository), ADMIN_EMAIL);
        patientMvc = build(new AuditPatientController(repository, userRepository), PATIENT_EMAIL);
    }

    private MockMvc build(Object controller, String email) {
        lenient().when(authentication.getName()).thenReturn(email);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
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
    void adminLogs_deberiaDevolverLogsFiltrados() throws Exception {
        adminMvc.perform(get("/admin/health/audit/logs").param("patientId", PATIENT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("VIEW_HISTORY"))
                .andExpect(jsonPath("$.content[0].ipAddress").value("1.2.3.4"));
    }

    @Test
    void adminLogs_sinResultados_deberiaDevolverVacio() throws Exception {
        adminMvc.perform(get("/admin/health/audit/logs").param("action", "CONFIRM_APPOINTMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void patientMyLogs_deberiaDevolverSoloSusLogs() throws Exception {
        patientMvc.perform(get("/health/audit/my-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].patientId").value(PATIENT.toString()));
    }
}

