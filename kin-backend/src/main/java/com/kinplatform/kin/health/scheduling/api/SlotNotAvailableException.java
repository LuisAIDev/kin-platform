package com.kinplatform.kin.health.scheduling.api;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El slot solicitado no está disponible (fuera de la disponibilidad del médico
 * o ya ocupado por otra cita).
 */
public class SlotNotAvailableException extends ResponseStatusException {

    public SlotNotAvailableException(LocalDateTime scheduledAt) {
        super(HttpStatus.CONFLICT, "El horario solicitado no está disponible: " + scheduledAt);
    }
}
