package com.kinplatform.kin.health.physician.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests del validador centralizado de acceso por estado de relación (Área 5):
 * solo las relaciones ACTIVE habilitan el acceso clínico.
 */
class RelationshipAccessValidatorTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    private static RelationshipAccessValidator validatorWith(RelationshipStatus status) {
        var repos = new InMemoryPhysicianRepositories();
        var assignment = InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT);
        switch (status) {
            case PENDING -> repos.patientRepository().assign(assignment);
            case ACTIVE -> repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
            case SUSPENDED -> repos.patientRepository().assign(assignment.suspended(OffsetDateTime.now()));
            case ENDED -> repos.patientRepository().assign(
                    assignment.ended(OffsetDateTime.now(), "REJECTED_BY_PATIENT"));
        }
        return new RelationshipAccessValidator(repos.patientRepository());
    }

    @Test
    void hasActiveRelationship_activa_deberiaSerTrue() {
        assertTrue(validatorWith(RelationshipStatus.ACTIVE).hasActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void hasActiveRelationship_pendiente_deberiaSerFalse() {
        assertFalse(validatorWith(RelationshipStatus.PENDING).hasActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void hasActiveRelationship_suspendida_deberiaSerFalse() {
        assertFalse(validatorWith(RelationshipStatus.SUSPENDED).hasActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void hasActiveRelationship_finalizada_deberiaSerFalse() {
        assertFalse(validatorWith(RelationshipStatus.ENDED).hasActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void hasRelationship_pendiente_deberiaSerTrue() {
        assertTrue(validatorWith(RelationshipStatus.PENDING).hasRelationship(PHYSICIAN, PATIENT, RelationshipStatus.PENDING));
    }

    @Test
    void requireActiveRelationship_activa_deberiaPasar() {
        assertDoesNotThrow(() -> validatorWith(RelationshipStatus.ACTIVE).requireActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void requireActiveRelationship_pendiente_deberiaLanzar() {
        assertThrows(
                RelationshipNotActiveException.class,
                () -> validatorWith(RelationshipStatus.PENDING).requireActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void requireActiveRelationship_finalizada_deberiaLanzar() {
        assertThrows(
                RelationshipNotActiveException.class,
                () -> validatorWith(RelationshipStatus.ENDED).requireActiveRelationship(PHYSICIAN, PATIENT));
    }

    @Test
    void requireActiveRelationshipBetween_direccionInversa_deberiaPasar() {
        // Simétrica: no importa el orden (paciente, médico) vs (médico, paciente).
        assertDoesNotThrow(() ->
                validatorWith(RelationshipStatus.ACTIVE).requireActiveRelationshipBetween(PATIENT, PHYSICIAN));
    }

    @Test
    void requireActiveRelationshipBetween_sinRelacion_deberiaLanzar() {
        var repos = new InMemoryPhysicianRepositories();
        var validator = new RelationshipAccessValidator(repos.patientRepository());

        assertThrows(
                RelationshipNotActiveException.class,
                () -> validator.requireActiveRelationshipBetween(PHYSICIAN, PATIENT));
    }

    @Test
    void requireActiveRelationshipBetween_pendiente_deberiaLanzar() {
        assertThrows(
                RelationshipNotActiveException.class,
                () -> validatorWith(RelationshipStatus.PENDING).requireActiveRelationshipBetween(PHYSICIAN, PATIENT));
    }
}
