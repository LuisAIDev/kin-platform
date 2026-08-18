package com.kinplatform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El manejador global preserva el código HTTP de {@link ResponseStatusException}
 * (usado por la exportación) en lugar de devolver 500.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatusPreservaElCodigoHttp() {
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento de referencia no encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Documento de referencia no encontrado", response.getBody().get("error"));
    }

    @Test
    void responseStatusCon400ConservaElMensaje() {
        var response = handler.handleResponseStatus(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "El documento seleccionado no puede utilizarse como plantilla."));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "El documento seleccionado no puede utilizarse como plantilla.",
                response.getBody().get("error"));
    }

    @Test
    void responseStatusSinReasonUsaMensajeGenerico() {
        var response = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("Solicitud inválida"));
    }

    @Test
    void tipoDeParametroInvalidoDevuelve400() {
        var response = handler.handleTypeMismatch(
                new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                        "no-es-uuid", java.util.UUID.class, "templateDocumentId", null, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("templateDocumentId"));
    }
}
