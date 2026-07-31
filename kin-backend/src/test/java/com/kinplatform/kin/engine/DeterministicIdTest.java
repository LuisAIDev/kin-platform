package com.kinplatform.kin.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterministicIdTest {

    @Test
    void from_deberiaSerDeterminista() {
        assertEquals(
            DeterministicId.from("BUSINESS", "Riesgo", "Descripción"),
            DeterministicId.from("BUSINESS", "Riesgo", "Descripción"));
    }

    @Test
    void from_deberiaVariarConLaEntrada() {
        assertNotEquals(
            DeterministicId.from("BUSINESS", "Riesgo", "Descripción"),
            DeterministicId.from("BUSINESS", "Riesgo", "Otra descripción"));
    }

    @Test
    void from_deberiaVariarConLaCategoria() {
        assertNotEquals(
            DeterministicId.from("BUSINESS", "Riesgo", "Descripción"),
            DeterministicId.from("TECHNICAL", "Riesgo", "Descripción"));
    }
}
