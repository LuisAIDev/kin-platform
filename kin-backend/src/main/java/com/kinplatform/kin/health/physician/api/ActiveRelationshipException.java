package com.kinplatform.kin.health.physician.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El paciente ya tiene una relación {@code ACTIVE} con el médico; no se permite
 * invitarlo ni reenviarle la invitación (evita duplicados en relaciones activas).
 */
public class ActiveRelationshipException extends ResponseStatusException {

    public ActiveRelationshipException() {
        super(HttpStatus.CONFLICT, "El paciente ya está vinculado a usted. No se puede invitar de nuevo.");
    }
}
