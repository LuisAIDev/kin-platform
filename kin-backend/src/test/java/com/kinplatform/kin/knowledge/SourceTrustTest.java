package com.kinplatform.kin.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceTrustTest {

    @Test
    void deberiaExponerLosTresNivelesDeConfianza() {
        assertEquals(3, SourceTrust.values().length);
        assertEquals("Fuente pública oficial", SourceTrust.OFFICIAL_PUBLIC.displayName());
        assertEquals("Fuente secundaria", SourceTrust.SECONDARY.displayName());
        assertEquals("Fuente no verificada", SourceTrust.UNVERIFIED.displayName());
    }

    @Test
    void deberiaReconstruirPorNombre() {
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, SourceTrust.valueOf("OFFICIAL_PUBLIC"));
        assertEquals(SourceTrust.SECONDARY, SourceTrust.valueOf("SECONDARY"));
        assertEquals(SourceTrust.UNVERIFIED, SourceTrust.valueOf("UNVERIFIED"));
    }
}
