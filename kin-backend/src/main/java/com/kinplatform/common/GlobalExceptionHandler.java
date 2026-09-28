package com.kinplatform.common;

import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.auth.EmailVerificationRequiredException;
import com.kinplatform.auth.PhysicianPendingReviewException;
import com.kinplatform.pricing.PlanNotFoundException;
import com.kinplatform.project.ProjectLimitExceededException;
import com.kinplatform.project.ReportNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        HttpStatus status = (msg != null && msg.contains("no encontrado")) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Preserva el código HTTP de los errores lanzados con
     * {@link ResponseStatusException} (p. ej. exportación: 404/400) en lugar de
     * que caigan en el manejador genérico de RuntimeException (500).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "Solicitud inválida";
        }
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", reason));
    }

    /**
     * Parámetros de ruta/query con tipo inválido (p. ej. un UUID malformado)
     * son un error del cliente: 400, no 500.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Parámetro inválido: " + ex.getName()));
    }

    @ExceptionHandler(EmailVerificationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleEmailVerificationRequired(EmailVerificationRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage(), "code", "EMAIL_VERIFICATION_REQUIRED"));
    }

    @ExceptionHandler(PhysicianPendingReviewException.class)
    public ResponseEntity<Map<String, String>> handlePhysicianPendingReview(PhysicianPendingReviewException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage(), "code", "ACCOUNT_PENDING_REVIEW"));
    }

    /**
     * Violación de constraint de BD (p. ej. dos registros concurrentes con el
     * mismo email). Respuesta genérica: no revela qué constraint se violó, pero
     * la causa raíz (constraint/columna) se registra en logs para diagnóstico.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolation (respuesta 409 genérica): {}", rootCauseMessage(ex));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "No se pudo completar la solicitud. Intenta de nuevo."));
    }

    /** Mensaje de la causa más profunda (p. ej. el constraint violado). */
    private static String rootCauseMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        String message = t.getMessage();
        return (message == null || message.isBlank()) ? ex.getMessage() : message;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    @ExceptionHandler(PlanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePlanNotFound(PlanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleReportNotFound(ReportNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "El archivo supera el tamaño máximo permitido."));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProjectLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleProjectLimit(ProjectLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(com.kinplatform.kin.usage.AiBudgetExceededException.class)
    public ResponseEntity<Map<String, String>> handleAiBudgetExceeded(
            com.kinplatform.kin.usage.AiBudgetExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(QuotaExceededException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "QUOTA_EXCEEDED");
        body.put("message", ex.getMessage());
        body.put("code", ex.getCode());
        if (ex.getRedirectUrl() != null) {
            body.put("redirectUrl", ex.getRedirectUrl());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
