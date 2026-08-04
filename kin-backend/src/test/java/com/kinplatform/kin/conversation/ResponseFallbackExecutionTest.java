package com.kinplatform.kin.conversation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Semántica de ejecución del {@link ResponseFallback} (ADR-017, Etapa E5):
 * decisión de reintento acotado y respuesta segura ante validaciones.
 */
class ResponseFallbackExecutionTest {

    @Test
    void validacionAceptada_deberiaNoSolicitarReintento() {
        var fallback = new ResponseFallback(List.of("segura"), 3);

        assertFalse(fallback.shouldRetry(ResponseValidation.ok(), 1));
    }

    @Test
    void validacionRechazada_conRetryPermitido_deberiaSolicitarReintento() {
        var fallback = new ResponseFallback(List.of("segura"), 2);
        var rejected = ResponseValidation.rejected(List.of("response.empty"));

        assertTrue(fallback.shouldRetry(rejected, 1));
        assertTrue(fallback.shouldRetry(rejected, 2));
        assertFalse(fallback.shouldRetry(rejected, 3));
    }

    @Test
    void validacionRechazada_sinRetry_deberiaDevolverRespuestaSegura() {
        var fallback = new ResponseFallback(List.of("respuesta segura"), 0);
        var rejected = ResponseValidation.rejected(List.of("issue"));

        assertFalse(fallback.shouldRetry(rejected, 1));
        assertEquals("respuesta segura", fallback.cannedResponse());
    }

    @Test
    void respuestaSegura_deberiaSerNoNulaYNoVacia() {
        var fallback = new ResponseFallback(List.of("segura"), 2);
        var rejected = ResponseValidation.rejected(List.of("issue"));

        String safe = fallback.cannedResponse(rejected);
        assertNotNull(safe);
        assertFalse(safe.isBlank());
    }

    @Test
    void respuestaSegura_deberiaOcultarElMotivoTecnico() {
        var fallback = new ResponseFallback();
        var rejected = ResponseValidation.rejected(List.of("response.empty"));

        assertFalse(fallback.cannedResponse(rejected).contains("response.empty"));
        assertEquals(ResponseFallback.DEFAULT_CANNED_RESPONSE, fallback.cannedResponse(rejected));
    }

    @Test
    void retryAgotado_deberiaDecidirRespuestaSegura() {
        var fallback = new ResponseFallback(List.of("segura"), 1);
        var rejected = ResponseValidation.rejected(List.of("issue"));

        assertFalse(fallback.shouldRetry(rejected, 2));
        assertEquals("segura", fallback.cannedResponse());
    }
}
