package com.kinplatform.kin.health.differential.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El módulo de diagnóstico diferencial está deshabilitado por configuración.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DifferentialDisabledException extends RuntimeException {

    public DifferentialDisabledException() {
        super("El módulo de diagnóstico diferencial está deshabilitado.");
    }
}
