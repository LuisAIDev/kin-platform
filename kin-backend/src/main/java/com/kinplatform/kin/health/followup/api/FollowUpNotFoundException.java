package com.kinplatform.kin.health.followup.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El recurso de seguimiento (plan, tarea o evolución) no existe.
 */
public class FollowUpNotFoundException extends ResponseStatusException {

    public FollowUpNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
