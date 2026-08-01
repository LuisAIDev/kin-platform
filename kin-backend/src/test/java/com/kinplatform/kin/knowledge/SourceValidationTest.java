package com.kinplatform.kin.knowledge;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceValidationTest {

    @Test
    void accepted_deberiaMarcarComoAceptada() {
        var validation = SourceValidation.accepted(SourceTrust.OFFICIAL_PUBLIC);

        assertTrue(validation.accepted());
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, validation.trust());
        assertTrue(validation.reasons().isEmpty());
    }

    @Test
    void rejected_deberiaMarcarComoRechazada() {
        var validation = SourceValidation.rejected("Dominio no permitido");

        assertFalse(validation.accepted());
        assertEquals(SourceTrust.UNVERIFIED, validation.trust());
        assertEquals(List.of("Dominio no permitido"), validation.reasons());
    }

    @Test
    void rejected_conRazonNula_deberiaNormalizarAVacio() {
        var validation = SourceValidation.rejected(null);

        assertFalse(validation.accepted());
        assertEquals(SourceTrust.UNVERIFIED, validation.trust());
        assertEquals(List.of(""), validation.reasons());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var validation = new SourceValidation(false, null, null);

        assertFalse(validation.accepted());
        assertEquals(SourceTrust.UNVERIFIED, validation.trust());
        assertTrue(validation.reasons().isEmpty());
    }

    @Test
    void constructor_deberiaConservarValores() {
        var validation = new SourceValidation(true, SourceTrust.SECONDARY, List.of("ok"));

        assertTrue(validation.accepted());
        assertEquals(SourceTrust.SECONDARY, validation.trust());
        assertEquals(List.of("ok"), validation.reasons());
    }

    @Test
    void constructor_deberiaProtegerLaLista() {
        var reasons = new ArrayList<>(List.of("motivo"));
        var validation = new SourceValidation(false, null, reasons);

        reasons.add("otro");
        assertThrows(UnsupportedOperationException.class,
            () -> validation.reasons().add("extra"));
        assertEquals(1, validation.reasons().size());
    }
}
