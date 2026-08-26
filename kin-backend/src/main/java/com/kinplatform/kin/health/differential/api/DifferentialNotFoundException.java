package com.kinplatform.kin.health.differential.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La consulta de triaje no existe o no pertenece al paciente autenticado.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DifferentialNotFoundException extends RuntimeException {

    public DifferentialNotFoundException(UUID consultationId) {
        super("Consulta de triaje no encontrada: " + consultationId);
    }
}
