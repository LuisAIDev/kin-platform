package com.kinplatform.kin.health.dashboard.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Tipo de recordatorio inválido (ADR-030).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidReminderTypeException extends RuntimeException {

    public InvalidReminderTypeException(String raw) {
        super("Tipo de recordatorio inválido: " + raw + " (esperado CITA, MEDICACION o GENERAL)");
    }
}
