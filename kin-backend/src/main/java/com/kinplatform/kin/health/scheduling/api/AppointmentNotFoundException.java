package com.kinplatform.kin.health.scheduling.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * La cita de agenda no existe.
 */
public class AppointmentNotFoundException extends ResponseStatusException {

    public AppointmentNotFoundException(UUID appointmentId) {
        super(HttpStatus.NOT_FOUND, "Cita no encontrada: " + appointmentId);
    }
}
