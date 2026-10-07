package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PatientAccessTest {

    private static User user(UserRole role, Boolean healthDataConsent) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("paciente@kin.com")
                .role(role)
                .healthDataConsent(healthDataConsent)
                .build();
    }

    @Test
    void patientLegacy_deberiaSerPaciente() {
        assertTrue(PatientAccess.isPatient(user(UserRole.PATIENT, false)));
        assertTrue(PatientAccess.isPatient(user(UserRole.PATIENT, null)));
        assertTrue(PatientAccess.isPatient(user(UserRole.PATIENT, true)));
    }

    @Test
    void freeConConsentimiento_deberiaSerPaciente() {
        assertTrue(PatientAccess.isPatient(user(UserRole.FREE, true)));
    }

    @Test
    void freeSinConsentimiento_noDeberiaSerPaciente() {
        assertFalse(PatientAccess.isPatient(user(UserRole.FREE, false)));
        assertFalse(PatientAccess.isPatient(user(UserRole.FREE, null)));
    }

    @Test
    void premiumConConsentimiento_deberiaSerPaciente() {
        assertTrue(PatientAccess.isPatient(user(UserRole.PREMIUM, true)));
    }

    @Test
    void premiumSinConsentimiento_noDeberiaSerPaciente() {
        assertFalse(PatientAccess.isPatient(user(UserRole.PREMIUM, false)));
    }

    @Test
    void facilitatorConConsentimiento_deberiaSerPaciente() {
        assertTrue(PatientAccess.isPatient(user(UserRole.FACILITADOR, true)));
    }

    @Test
    void physicianConConsentimiento_deberiaSerPaciente() {
        assertTrue(PatientAccess.isPatient(user(UserRole.PHYSICIAN, true)));
    }

    @Test
    void physicianSinConsentimiento_noDeberiaSerPaciente() {
        assertFalse(PatientAccess.isPatient(user(UserRole.PHYSICIAN, false)));
    }

    @Test
    void adminConConsentimiento_deberiaSerPaciente() {
        assertTrue(PatientAccess.isPatient(user(UserRole.ADMIN, true)));
    }

    @Test
    void adminSinConsentimiento_noDeberiaSerPaciente() {
        assertFalse(PatientAccess.isPatient(user(UserRole.ADMIN, false)));
    }

    @Test
    void usuarioNulo_noDeberiaSerPaciente() {
        assertFalse(PatientAccess.isPatient(null));
    }
}

