package com.kinplatform.kin.health.telemedicine.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El médico no está asignado al paciente (aislamiento estricto).
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TelemedicineAssignmentException extends RuntimeException {

    public TelemedicineAssignmentException(UUID a, UUID b) {
        super("No hay una asignación médico-paciente válida entre " + a + " y " + b);
    }
}
