package com.kinplatform.kin.health.physician.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ya existe una relación ACTIVE o PENDING entre el médico y el paciente;
 * no se permite duplicar la invitación.
 */
public class DuplicateRelationshipException extends ResponseStatusException {

    public DuplicateRelationshipException() {
        super(HttpStatus.CONFLICT,
                "Ya existe una invitación o relación activa con este paciente.");
    }
}
