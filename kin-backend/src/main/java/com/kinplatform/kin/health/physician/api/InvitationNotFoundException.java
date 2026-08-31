package com.kinplatform.kin.health.physician.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * No existe una invitación pendiente entre el paciente y el médico (o ya fue
 * aceptada/rechazada). Aislamiento: el paciente solo ve sus propias
 * invitaciones.
 */
public class InvitationNotFoundException extends ResponseStatusException {

    public InvitationNotFoundException(UUID physicianId, UUID patientId) {
        super(HttpStatus.NOT_FOUND,
                "No existe una invitación pendiente del médico " + physicianId + " para el paciente " + patientId);
    }
}
