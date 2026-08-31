package com.kinplatform.kin.health.followup.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El módulo de seguimiento de pacientes está deshabilitado
 * ({@code kin.health.followup.enabled=false}).
 */
public class FollowUpDisabledException extends ResponseStatusException {

    public FollowUpDisabledException() {
        super(HttpStatus.NOT_FOUND, "El módulo de seguimiento está deshabilitado");
    }
}
