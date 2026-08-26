package com.kinplatform.kin.health.triage.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El módulo de triaje está deshabilitado por configuración
 * ({@code kin.health.triage.enabled=false}).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TriageDisabledException extends RuntimeException {

    public TriageDisabledException() {
        super("El módulo de triaje digital está deshabilitado.");
    }
}
