package com.kinplatform.kin.health.physician.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El paciente no está asignado al médico (aislamiento estricto).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PhysicianPatientNotFoundException extends RuntimeException {

    public PhysicianPatientNotFoundException(UUID patientId) {
        super("Paciente no asignado a este médico: " + patientId);
    }
}
