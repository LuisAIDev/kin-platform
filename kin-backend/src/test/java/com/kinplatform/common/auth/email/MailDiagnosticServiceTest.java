package com.kinplatform.common.auth.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MailDiagnosticServiceTest {

    private MailDiagnosticService service;

    @BeforeEach
    void setUp() {
        service = new MailDiagnosticService(null);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "host", "127.0.0.1");
        ReflectionTestUtils.setField(service, "port", 1);
        ReflectionTestUtils.setField(service, "username", "user@kin.com");
        ReflectionTestUtils.setField(service, "password", "secret");
        ReflectionTestUtils.setField(service, "smtpAuth", true);
        ReflectionTestUtils.setField(service, "from", "hola@kin-platform.com");
        ReflectionTestUtils.setField(service, "fromName", "KIN Platform");
    }

    @Test
    void maskEmail_deberiaEnmascararLocalPart() {
        assertEquals("h**a@kin-platform.com", MailDiagnosticService.maskEmail("hola@kin-platform.com"));
        assertEquals("**@kin.com", MailDiagnosticService.maskEmail("ab@kin.com"));
        assertEquals("(sin configurar)", MailDiagnosticService.maskEmail(null));
    }

    @Test
    void diagnose_conHostInalcanzable_deberiaReportarFalloDeConexion() {
        var result = service.diagnose(null);

        assertFalse(result.connected());
        assertNotNull(result.connectionError());
        assertEquals("127.0.0.1", result.config().get("host"));
        assertEquals("h**a@kin-platform.com", result.config().get("from"));
        assertEquals("u***m", result.config().get("username"));
        assertFalse(result.testEmailSent());
        assertFalse(result.logs().isEmpty());
    }

    @Test
    void diagnose_conDestinatarioPide_sinConexion_noIntentaEnviar() {
        var result = service.diagnose("test@kin.com");

        assertFalse(result.connected());
        assertFalse(result.testEmailSent());
        assertTrue(result.connectionError() != null);
    }
}

