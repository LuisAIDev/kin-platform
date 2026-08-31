package com.kinplatform.kin.health.physician.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El email no corresponde a un paciente registrado en KIN (o no tiene rol
 * PATIENT). El médico solo puede invitar a pacientes existentes.
 */
public class PatientNotRegisteredException extends ResponseStatusException {

    public PatientNotRegisteredException(String email) {
        super(HttpStatus.NOT_FOUND, "Paciente no registrado en KIN: " + email);
    }
}
