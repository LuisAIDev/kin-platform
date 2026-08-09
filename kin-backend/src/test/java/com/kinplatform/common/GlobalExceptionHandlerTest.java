package com.kinplatform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinplatform.auth.EmailVerificationRequiredException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void emailVerificationRequired_deberiaResponder403ConCodigo() {
        var ex = new EmailVerificationRequiredException("Tu correo aún no ha sido verificado.");

        var response = handler.handleEmailVerificationRequired(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("EMAIL_VERIFICATION_REQUIRED", response.getBody().get("code"));
        assertEquals("Tu correo aún no ha sido verificado.", response.getBody().get("error"));
    }

    @Test
    void dataIntegrity_deberiaResponder409Generico() {
        var ex = new org.springframework.dao.DataIntegrityViolationException("duplicate key");

        var response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No se pudo completar la solicitud. Intenta de nuevo.", response.getBody().get("error"));
    }

    @Test
    void illegalArgument_deberiaResponder400() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("boom"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "boom"), response.getBody());
    }
}
