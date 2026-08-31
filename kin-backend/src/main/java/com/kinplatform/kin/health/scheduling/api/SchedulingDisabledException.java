package com.kinplatform.kin.health.scheduling.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El módulo de agenda y disponibilidad está deshabilitado
 * ({@code kin.health.scheduling.enabled=false}).
 */
public class SchedulingDisabledException extends ResponseStatusException {

    public SchedulingDisabledException() {
        super(HttpStatus.NOT_FOUND, "El módulo de agenda está deshabilitado");
    }
}
