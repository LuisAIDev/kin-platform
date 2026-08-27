package com.kinplatform.auth.email;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MailDiagnosticControllerTest {

    @Mock
    private MailDiagnosticService mailDiagnosticService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MailDiagnosticController(mailDiagnosticService))
                .build();
    }

    @Test
    void diagnostic_deberiaDevolverConfigYLogs() throws Exception {
        when(mailDiagnosticService.diagnose(null))
                .thenReturn(new MailDiagnosticService.DiagnosticResult(
                        Map.of(
                                "enabled",
                                true,
                                "host",
                                "smtp-relay.brevo.com",
                                "port",
                                587,
                                "from",
                                "h**a@kin-platform.com",
                                "smtpAuth",
                                true),
                        true,
                        null,
                        true,
                        null,
                        List.of("Conexión SMTP OK a smtp-relay.brevo.com:587", "Correo de prueba enviado")));

        mockMvc.perform(get("/admin/health/email/diagnostic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.testEmailSent").value(true))
                .andExpect(jsonPath("$.config.host").value("smtp-relay.brevo.com"))
                .andExpect(jsonPath("$.config.from").value("h**a@kin-platform.com"))
                .andExpect(jsonPath("$.logs[0]").value("Conexión SMTP OK a smtp-relay.brevo.com:587"));
    }

    @Test
    void diagnostic_conDestinatario_deberiaPasarloAlServicio() throws Exception {
        when(mailDiagnosticService.diagnose("test@kin-platform.com"))
                .thenReturn(new MailDiagnosticService.DiagnosticResult(
                        Map.of("enabled", false), false, "Connection refused", false, null, List.of()));

        mockMvc.perform(get("/admin/health/email/diagnostic").param("to", "test@kin-platform.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.connectionError").value("Connection refused"));
    }
}
