package com.kinplatform.kin.health.scheduling.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El usuario autenticado no tiene permiso para operar sobre esta cita o
 * disponibilidad (no es el paciente ni el médico implicado).
 */
public class AppointmentAccessDeniedException extends ResponseStatusException {

    public AppointmentAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
