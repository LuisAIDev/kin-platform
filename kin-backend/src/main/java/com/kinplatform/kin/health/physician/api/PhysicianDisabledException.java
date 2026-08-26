package com.kinplatform.kin.health.physician.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El módulo de portal de médicos está deshabilitado por configuración.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PhysicianDisabledException extends RuntimeException {

    public PhysicianDisabledException() {
        super("El módulo de portal de médicos está deshabilitado.");
    }
}
