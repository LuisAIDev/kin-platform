package com.kinplatform.kin.health.physician.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * La cuenta existe en KIN pero no tiene capacidad de paciente: falta el
 * consentimiento de datos de salud ({@code health_data_consent = true}, ADR-040).
 */
public class PatientNotCapableException extends ResponseStatusException {

    public PatientNotCapableException() {
        super(HttpStatus.CONFLICT,
                "La cuenta existe pero no tiene capacidad de paciente. Debe aceptar el consentimiento de datos de salud.");
    }
}
