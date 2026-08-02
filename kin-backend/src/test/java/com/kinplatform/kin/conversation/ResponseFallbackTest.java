package com.kinplatform.kin.conversation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseFallbackTest {

    @Test
    void constructorPorDefecto_deberiaUsarRespuestaYReintentoPorDefecto() {
        var fallback = new ResponseFallback();

        assertEquals(ResponseFallback.DEFAULT_CANNED_RESPONSE, fallback.cannedResponse());
        assertEquals(1, fallback.maxRetries());
        assertEquals(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), fallback.cannedResponses());
    }

    @Test
    void constructorCustom_deberiaConservarRespuestasYReintentos() {
        var fallback = new ResponseFallback(List.of("respuesta A", "respuesta B"), 3);

        assertEquals("respuesta A", fallback.cannedResponse());
        assertEquals(3, fallback.maxRetries());
        assertEquals(2, fallback.cannedResponses().size());
    }

    @Test
    void constructorCustom_deberiaNormalizarListaVaciaAPorDefecto() {
        var fallback = new ResponseFallback(List.of(), 2);

        assertEquals(ResponseFallback.DEFAULT_CANNED_RESPONSE, fallback.cannedResponse());
    }

    @Test
    void constructorCustom_deberiaNormalizarListaNulaAPorDefecto() {
        var fallback = new ResponseFallback(null, 2);

        assertEquals(ResponseFallback.DEFAULT_CANNED_RESPONSE, fallback.cannedResponse());
    }

    @Test
    void constructorCustom_deberiaLimpiarMaxRetriesNegativoACero() {
        var fallback = new ResponseFallback(List.of("x"), -5);

        assertEquals(0, fallback.maxRetries());
    }

    @Test
    void constructorCustom_deberiaCopiarLaListaDefensivamente() {
        var mutable = new java.util.ArrayList<>(List.of("x", "y"));
        var fallback = new ResponseFallback(mutable, 1);
        mutable.add("z");

        assertEquals(2, fallback.cannedResponses().size());
        assertNotSame(mutable, fallback.cannedResponses());
    }

    @Test
    void cannedResponse_conValidacionRechazada_deberiaIncluirElMotivo() {
        var fallback = new ResponseFallback();
        var validation = ResponseValidation.rejected(List.of("respuesta vacía"));

        var response = fallback.cannedResponse(validation);

        assertTrue(response.contains("respuesta vacía"));
        assertTrue(response.contains(ResponseFallback.DEFAULT_CANNED_RESPONSE));
    }

    @Test
    void cannedResponse_conValidacionAceptada_deberiaDevolverLaPorDefecto() {
        var fallback = new ResponseFallback();

        assertEquals(fallback.cannedResponse(), fallback.cannedResponse(ResponseValidation.ok()));
    }

    @Test
    void cannedResponse_conValidacionNula_deberiaDevolverLaPorDefecto() {
        var fallback = new ResponseFallback();

        assertEquals(fallback.cannedResponse(), fallback.cannedResponse(null));
    }

    @Test
    void shouldRetry_deberiaPermitirReintentoDentroDelLimite() {
        var fallback = new ResponseFallback(List.of("x"), 2);
        var rejected = ResponseValidation.rejected(List.of("issue"));

        assertTrue(fallback.shouldRetry(rejected, 1));
        assertTrue(fallback.shouldRetry(rejected, 2));
        assertFalse(fallback.shouldRetry(rejected, 3));
    }

    @Test
    void shouldRetry_conValidacionAceptada_deberiaSerFalso() {
        var fallback = new ResponseFallback(List.of("x"), 3);

        assertFalse(fallback.shouldRetry(ResponseValidation.ok(), 1));
    }

    @Test
    void shouldRetry_conValidacionNula_deberiaSerFalso() {
        var fallback = new ResponseFallback(List.of("x"), 3);

        assertFalse(fallback.shouldRetry(null, 1));
    }

    @Test
    void shouldRetry_conIntentoNoPositivo_deberiaSerFalso() {
        var fallback = new ResponseFallback(List.of("x"), 3);
        var rejected = ResponseValidation.rejected(List.of("issue"));

        assertFalse(fallback.shouldRetry(rejected, 0));
        assertFalse(fallback.shouldRetry(rejected, -1));
    }

    @Test
    void shouldRetry_sinReintentos_deberiaSerFalso() {
        var fallback = new ResponseFallback(List.of("x"), 0);
        var rejected = ResponseValidation.rejected(List.of("issue"));

        assertFalse(fallback.shouldRetry(rejected, 1));
    }

    @Test
    void cannedResponses_deberiaSerInmutable() {
        var fallback = new ResponseFallback(List.of("x", "y"), 1);

        assertThrows(UnsupportedOperationException.class,
            () -> fallback.cannedResponses().add("z"));
    }
}
