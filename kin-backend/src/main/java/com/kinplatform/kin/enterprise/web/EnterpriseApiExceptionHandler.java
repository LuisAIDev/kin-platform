package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProjectException;
import com.kinplatform.kin.enterprise.application.EnterpriseExportException;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapeo global de excepciones de la API Enterprise (Fase 10, Milestone 2I).
 *
 * <p>Traduce las excepciones de dominio y de la capa web a respuestas HTTP con
 * el cuerpo uniforme {@link EnterpriseApiError}:</p>
 *
 * <ul>
 *   <li>{@link EnterpriseNotFoundException} y {@link EnterpriseExportException}
 *       → 404 Not Found.</li>
 *   <li>{@link EnterpriseProjectException} → 409 Conflict (invariante de estado).</li>
 *   <li>{@link EnterpriseUnprocessableEntityException} → 422 Unprocessable Entity.</li>
 *   <li>{@link IllegalArgumentException}, errores de validación y de conversión
 *       → 400 Bad Request.</li>
 *   <li>Cualquier otra excepción → 500 Internal Server Error (logeada).</li>
 * </ul>
 */
@RestControllerAdvice
public class EnterpriseApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseApiExceptionHandler.class);

    @ExceptionHandler(EnterpriseNotFoundException.class)
    public ResponseEntity<EnterpriseApiError> handleNotFound(EnterpriseNotFoundException ex,
                                                             HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EnterpriseExportException.class)
    public ResponseEntity<EnterpriseApiError> handleExport(EnterpriseExportException ex,
                                                           HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EnterpriseProjectException.class)
    public ResponseEntity<EnterpriseApiError> handleProject(EnterpriseProjectException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EnterpriseUnprocessableEntityException.class)
    public ResponseEntity<EnterpriseApiError> handleUnprocessable(EnterpriseUnprocessableEntityException ex,
                                                                  HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EnterpriseApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                                    HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EnterpriseApiError> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (var fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Validación de la solicitud fallida", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<EnterpriseApiError> handleConstraint(ConstraintViolationException ex,
                                                               HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (var violation : ex.getConstraintViolations()) {
            fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Validación de la solicitud fallida", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<EnterpriseApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                 HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
            "El parámetro '" + ex.getName() + "' no es válido: " + ex.getValue(), request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<EnterpriseApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EnterpriseApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en la API Enterprise", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
            "Error interno del servidor", request, Map.of());
    }

    private ResponseEntity<EnterpriseApiError> error(HttpStatus status, String message,
                                                     HttpServletRequest request,
                                                     Map<String, String> fieldErrors) {
        EnterpriseApiError body = new EnterpriseApiError(
            Instant.now(), status.value(), status.getReasonPhrase(), message,
            request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
