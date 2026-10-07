package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.user.PhysicianVerificationStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhysicianAccessTest {

    private static User user(UserRole role, PhysicianVerificationStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("medico@kin.com")
                .role(role)
                .physicianVerificationStatus(status)
                .build();
    }

    @Test
    void physicianLegacyConStatusNull_deberiaSerMedico() {
        assertTrue(PhysicianAccess.isPhysician(user(UserRole.PHYSICIAN, null)));
    }

    @Test
    void physicianLegacyAprobado_deberiaSerMedico() {
        assertTrue(PhysicianAccess.isPhysician(user(UserRole.PHYSICIAN, PhysicianVerificationStatus.APPROVED)));
    }

    @Test
    void physicianLegacyPendiente_noDeberiaSerMedico() {
        assertFalse(PhysicianAccess.isPhysician(user(UserRole.PHYSICIAN, PhysicianVerificationStatus.PENDING)));
    }

    @Test
    void physicianLegacyRechazado_noDeberiaSerMedico() {
        assertFalse(PhysicianAccess.isPhysician(user(UserRole.PHYSICIAN, PhysicianVerificationStatus.REJECTED)));
    }

    @Test
    void freeAprobado_deberiaSerMedico() {
        assertTrue(PhysicianAccess.isPhysician(user(UserRole.FREE, PhysicianVerificationStatus.APPROVED)));
    }

    @Test
    void premiumAprobado_deberiaSerMedico() {
        assertTrue(PhysicianAccess.isPhysician(user(UserRole.PREMIUM, PhysicianVerificationStatus.APPROVED)));
    }

    @Test
    void patientAprobado_deberiaSerMedico() {
        assertTrue(PhysicianAccess.isPhysician(user(UserRole.PATIENT, PhysicianVerificationStatus.APPROVED)));
    }

    @Test
    void freePendiente_noDeberiaSerMedico() {
        assertFalse(PhysicianAccess.isPhysician(user(UserRole.FREE, PhysicianVerificationStatus.PENDING)));
    }

    @Test
    void freeRechazado_noDeberiaSerMedico() {
        assertFalse(PhysicianAccess.isPhysician(user(UserRole.FREE, PhysicianVerificationStatus.REJECTED)));
    }

    @Test
    void freeSinSolicitud_noDeberiaSerMedico() {
        assertFalse(PhysicianAccess.isPhysician(user(UserRole.FREE, null)));
    }

    @Test
    void facilitatorAprobado_deberiaSerMedico() {
        assertTrue(PhysicianAccess.isPhysician(user(UserRole.FACILITADOR, PhysicianVerificationStatus.APPROVED)));
    }

    @Test
    void usuarioNulo_noDeberiaSerMedico() {
        assertFalse(PhysicianAccess.isPhysician(null));
    }
}

