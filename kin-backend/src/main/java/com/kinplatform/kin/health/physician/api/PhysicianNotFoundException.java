package com.kinplatform.kin.health.physician.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El usuario que invita no es un médico registrado (rol PHYSICIAN).
 */
public class PhysicianNotFoundException extends ResponseStatusException {

    public PhysicianNotFoundException(UUID physicianId) {
        super(HttpStatus.NOT_FOUND, "Médico no encontrado: " + physicianId);
    }
}
