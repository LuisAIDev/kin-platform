package com.kinplatform.kin.health.physician.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El correo no corresponde a ninguna cuenta KIN. El médico solo puede invitar
 * a pacientes que ya tienen cuenta registrada en la plataforma.
 */
public class PatientNotRegisteredException extends ResponseStatusException {

    public PatientNotRegisteredException() {
        super(HttpStatus.NOT_FOUND,
                "No existe una cuenta KIN con ese correo. El paciente debe registrarse antes de ser invitado.");
    }
}
