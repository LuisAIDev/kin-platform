package com.kinplatform.kin.health.physician.access;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * La relación médico-paciente no está {@code ACTIVE} (pendiente de aceptación,
 * suspendida o finalizada), por lo que el médico no tiene permiso para acceder
 * a los datos de salud del paciente.
 *
 * <p>Es la excepción de acceso central del {@link RelationshipAccessValidator}:
 * solo las relaciones {@code ACTIVE} habilitan historial, mensajería, citas y
 * alertas (Área 5 de la hoja de ruta KIN Salud 2.0).</p>
 */
public class RelationshipNotActiveException extends ResponseStatusException {

    public RelationshipNotActiveException(UUID physicianId, UUID patientId) {
        super(HttpStatus.FORBIDDEN,
                "El paciente no ha aceptado la relación o la relación no está activa (médico " + physicianId
                        + ", paciente " + patientId + ")");
    }

    public RelationshipNotActiveException(UUID a, UUID b, String reason) {
        super(HttpStatus.FORBIDDEN, reason + " (" + a + ", " + b + ")");
    }
}
