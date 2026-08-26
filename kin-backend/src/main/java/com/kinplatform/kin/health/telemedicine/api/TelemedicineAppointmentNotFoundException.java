package com.kinplatform.kin.health.telemedicine.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La cita no existe o el usuario no tiene acceso (aislamiento).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TelemedicineAppointmentNotFoundException extends RuntimeException {

    public TelemedicineAppointmentNotFoundException(UUID appointmentId) {
        super("Cita no encontrada: " + appointmentId);
    }
}
