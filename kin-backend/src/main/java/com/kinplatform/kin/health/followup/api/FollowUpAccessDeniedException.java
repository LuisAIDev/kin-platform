package com.kinplatform.kin.health.followup.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El usuario autenticado no tiene permiso para operar sobre el recurso de
 * seguimiento (no es el médico dueño del plan ni el paciente implicado).
 */
public class FollowUpAccessDeniedException extends ResponseStatusException {

    public FollowUpAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
