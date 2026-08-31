package com.kinplatform.kin.health.physician.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ya existe una relación ACTIVE o PENDING entre el médico y el paciente;
 * no se permite duplicar la invitación.
 */
public class DuplicateRelationshipException extends ResponseStatusException {

    public DuplicateRelationshipException(UUID physicianId, UUID patientId) {
        super(HttpStatus.CONFLICT,
                "Ya existe una relación (ACTIVE o PENDING) entre el médico " + physicianId + " y el paciente "
                        + patientId);
    }
}
