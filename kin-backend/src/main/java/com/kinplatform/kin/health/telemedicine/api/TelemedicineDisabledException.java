package com.kinplatform.kin.health.telemedicine.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El módulo de telemedicina está deshabilitado por configuración.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TelemedicineDisabledException extends RuntimeException {

    public TelemedicineDisabledException() {
        super("El módulo de telemedicina está deshabilitado.");
    }
}
