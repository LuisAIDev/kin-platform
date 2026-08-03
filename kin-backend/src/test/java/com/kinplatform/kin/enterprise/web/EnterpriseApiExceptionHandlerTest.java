package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProjectException;
import com.kinplatform.kin.enterprise.application.EnterpriseExportException;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseApiExceptionHandlerTest {

    private final EnterpriseApiExceptionHandler handler = new EnterpriseApiExceptionHandler();
    private final MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/enterprise/3fa85f64-5717-4562-b3fc-2c963f66afa6/5");

    @Test
    void handleNotFound_deberiaDevolver404() {
        var response = handler.handleNotFound(
            new EnterpriseNotFoundException("No existe la versión 5"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("No existe la versión 5", response.getBody().message());
        assertEquals(request.getRequestURI(), response.getBody().path());
        assertTrue(response.getBody().fieldErrors().isEmpty());
    }

    @Test
    void handleExport_deberiaDevolver404() {
        var response = handler.handleExport(
            new EnterpriseExportException("No existe la versión"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleProject_deberiaDevolver409() {
        var response = handler.handleProject(
            new EnterpriseProjectException("Transición no permitida"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleUnprocessable_deberiaDevolver422() {
        var response = handler.handleUnprocessable(
            new EnterpriseUnprocessableEntityException("Sin contexto"), request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    void handleIllegalArgument_deberiaDevolver400() {
        var response = handler.handleIllegalArgument(
            new IllegalArgumentException("Parámetro inválido"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parámetro inválido", response.getBody().message());
    }

    @Test
    void handleConstraint_deberiaDevolver400ConErroresPorCampo() {
        var violations = jakarta.validation.Validation.buildDefaultValidatorFactory()
            .getValidator()
            .validate(new com.kinplatform.kin.enterprise.web.dto.EnterpriseGenerateRequest(null, null));
        var response = handler.handleConstraint(
            new ConstraintViolationException("Validación fallida", violations), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().fieldErrors().containsKey("async"));
    }

    @Test
    void handleTypeMismatch_deberiaDevolver400() {
        var response = handler.handleTypeMismatch(
            new MethodArgumentTypeMismatchException("NO_EXISTE", DocumentType.class,
                "type", null, null), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("type"));
    }

    @Test
    void handleNotReadable_deberiaDevolver400() {
        var response = handler.handleNotReadable(
            new HttpMessageNotReadableException("Cuerpo inválido"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleGeneric_deberiaDevolver500() {
        var response = handler.handleGeneric(
            new IllegalStateException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", response.getBody().message());
    }

    @Test
    void apiError_conFieldErrorsNulos_deberiaNormalizarAVacio() {
        var error = new com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError(
            java.time.Instant.now(), 400, "Bad Request", "mensaje", "/ruta", null);

        assertTrue(error.fieldErrors().isEmpty());
    }
}
