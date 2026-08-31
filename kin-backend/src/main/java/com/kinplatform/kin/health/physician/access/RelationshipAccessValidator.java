package com.kinplatform.kin.health.physician.access;

import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Validador centralizado de acceso del médico a los datos de salud de un
 * paciente, basado en el <strong>estado de la relación</strong> (Área 5 de la
 * hoja de ruta KIN Salud 2.0).
 *
 * <p>Regla principal: solo las relaciones {@code ACTIVE} permiten acceso
 * completo (historial, mensajería, citas, alertas). Las relaciones
 * {@code PENDING}, {@code SUSPENDED} o {@code ENDED} no permiten acceso a
 * ningún dato sensible.</p>
 *
 * <p>Es un bean Spring ({@code relationshipAccessValidator}) inyectable en
 * cualquier servicio que necesite validar acceso; el método booleano
 * {@link #hasActiveRelationship(UUID, UUID)} también está disponible para uso
 * declarativo (p. ej. {@code @PreAuthorize}) si se habilita method security.</p>
 */
@Component
public class RelationshipAccessValidator {

    private final PhysicianPatientRepository physicianPatientRepository;

    public RelationshipAccessValidator(PhysicianPatientRepository physicianPatientRepository) {
        this.physicianPatientRepository = physicianPatientRepository;
    }

    /** {@code true} si existe una relación ACTIVA (único estado que habilita acceso clínico). */
    public boolean hasActiveRelationship(UUID physicianId, UUID patientId) {
        return physicianPatientRepository.isAssigned(physicianId, patientId);
    }

    /** {@code true} si existe una relación en el estado indicado (p. ej. PENDING). */
    public boolean hasRelationship(UUID physicianId, UUID patientId, RelationshipStatus status) {
        return physicianPatientRepository.existsByPhysicianIdAndPatientIdAndStatus(physicianId, patientId, status);
    }

    /**
     * Valida acceso clínico direccional (médico → paciente). Lanza
     * {@link RelationshipNotActiveException} si la relación no está ACTIVE.
     */
    public void requireActiveRelationship(UUID physicianId, UUID patientId) {
        if (!hasActiveRelationship(physicianId, patientId)) {
            throw new RelationshipNotActiveException(physicianId, patientId);
        }
    }

    /**
     * Valida acceso simétrico entre dos participantes (emisor/receptor de un
     * mensaje o de una cita): basta con que haya una relación ACTIVA en
     * cualquiera de las dos direcciones.
     */
    public void requireActiveRelationshipBetween(UUID a, UUID b) {
        if (!hasActiveRelationship(a, b) && !hasActiveRelationship(b, a)) {
            throw new RelationshipNotActiveException(a, b, "No hay una relación médico-paciente activa entre");
        }
    }
}
