package com.kinplatform.kin.health.dashboard.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El módulo de dashboard de salud está deshabilitado por configuración.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DashboardDisabledException extends RuntimeException {

    public DashboardDisabledException() {
        super("El módulo de dashboard de salud está deshabilitado.");
    }
}
