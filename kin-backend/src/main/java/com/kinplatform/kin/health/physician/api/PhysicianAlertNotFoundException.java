package com.kinplatform.kin.health.physician.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La alerta no existe o no pertenece al médico.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PhysicianAlertNotFoundException extends RuntimeException {

    public PhysicianAlertNotFoundException(UUID alertId) {
        super("Alerta no encontrada: " + alertId);
    }
}
