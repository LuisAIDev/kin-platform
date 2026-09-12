package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlatformAccessTest {

    @Test
    void empresaPuedeAccederARutasEmpresas() {
        assertTrue(PlatformAccess.canAccess("EMPRESAS", "FREE", "/empresas/projects/abc"));
    }

    @Test
    void empresaNoPuedeAccederARutasHealth() {
        assertFalse(PlatformAccess.canAccess("EMPRESAS", "FREE", "/health/triage/consulta"));
    }

    @Test
    void empresaNoPuedeAccederARutasMedical() {
        assertFalse(PlatformAccess.canAccess("EMPRESAS", "FREE", "/medical/dashboard/datos"));
    }

    @Test
    void saludPersonalPuedeAccederARutasHealth() {
        assertTrue(PlatformAccess.canAccess("SALUD_PERSONAL", "PATIENT", "/health/triage/consulta"));
    }

    @Test
    void saludPersonalPuedeAccederARutasMedical() {
        assertTrue(PlatformAccess.canAccess("SALUD_PERSONAL", "PATIENT", "/medical/dashboard/datos"));
    }

    @Test
    void saludPersonalNoPuedeAccederARutasEmpresas() {
        assertFalse(PlatformAccess.canAccess("SALUD_PERSONAL", "PATIENT", "/empresas/projects/abc"));
    }

    @Test
    void saludProfesionalPuedeAccederARutasHealth() {
        assertTrue(PlatformAccess.canAccess("SALUD_PROFESIONAL", "PHYSICIAN", "/health/triage/consulta"));
    }

    @Test
    void saludProfesionalNoPuedeAccederARutasEmpresas() {
        assertFalse(PlatformAccess.canAccess("SALUD_PROFESIONAL", "PHYSICIAN", "/empresas/projects/abc"));
    }

    @Test
    void adminConPlatformEmpresasPuedeAccederHealth() {
        assertTrue(PlatformAccess.canAccess("EMPRESAS", "ADMIN", "/health/triage/consulta"));
    }

    @Test
    void adminConPlatformEmpresasPuedeAccederMedical() {
        assertTrue(PlatformAccess.canAccess("EMPRESAS", "ADMIN", "/medical/dashboard/datos"));
    }

    @Test
    void adminConPlatformSaludPuedeAccederEmpresas() {
        assertTrue(PlatformAccess.canAccess("SALUD_PERSONAL", "ADMIN", "/empresas/projects/abc"));
    }

    @Test
    void adminConPlatformSaludPuedeAccederTodo() {
        assertTrue(PlatformAccess.canAccess("SALUD_PROFESIONAL", "ADMIN", "/empresas/projects/abc"));
        assertTrue(PlatformAccess.canAccess("SALUD_PROFESIONAL", "ADMIN", "/health/triage/consulta"));
        assertTrue(PlatformAccess.canAccess("SALUD_PROFESIONAL", "ADMIN", "/medical/dashboard/datos"));
    }

    @Test
    void nullPlatformRetornaFalso() {
        assertFalse(PlatformAccess.canAccess(null, "FREE", "/health/triage"));
    }

    @Test
    void nullRequestPathRetornaFalso() {
        assertFalse(PlatformAccess.canAccess("EMPRESAS", "FREE", null));
    }

    @Test
    void rutaSinPrefijoEnMapaPermiteAcceso() {
        assertTrue(PlatformAccess.canAccess("EMPRESAS", "FREE", "/pricing-plans/basic"));
        assertTrue(PlatformAccess.canAccess("SALUD_PERSONAL", "PATIENT", "/pricing-plans/basic"));
        assertTrue(PlatformAccess.isUnrestricted("/auth/login"));
        assertTrue(PlatformAccess.isUnrestricted("/actuator/health"));
    }

    /**
     * Documenta el comportamiento ACTUAL con context-path: la ruta cruda
     * incluye /api/v1, ningún prefijo coincide y todo se considera
     * "unrestricted" (aislamiento inactivo). Es intencionalmente permisivo
     * para no romper ADR-039/040 hasta reconciliar el modelo.
     */
    @Test
    void conContextPathSinNormalizar_todoEsUnrestricted() {
        assertTrue(PlatformAccess.isUnrestricted("/api/v1/health/triage"));
        assertTrue(PlatformAccess.canAccess("EMPRESAS", "FREE", "/api/v1/health/triage"));
    }
}
