package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueObjectsTest {

    // ------------------------------------------------------------------
    // requireNotBlank
    // ------------------------------------------------------------------

    @Test
    void requireNotBlank_deberiaDevolverElValor() {
        assertEquals("texto", ValueObjects.requireNotBlank("texto", "campo"));
    }

    @Test
    void requireNotBlank_conNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.requireNotBlank(null, "campo"));
    }

    @Test
    void requireNotBlank_conBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.requireNotBlank("   ", "campo"));
    }

    // ------------------------------------------------------------------
    // immutableNotBlank
    // ------------------------------------------------------------------

    @Test
    void immutableNotBlank_deberiaDevolverListaInmutable() {
        var result = ValueObjects.immutableNotBlank(List.of("a", "b"), "campo");
        assertEquals(List.of("a", "b"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
    }

    @Test
    void immutableNotBlank_conNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.immutableNotBlank(null, "campo"));
    }

    @Test
    void immutableNotBlank_conElementoNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> ValueObjects.immutableNotBlank(java.util.Arrays.asList("a", null), "campo"));
    }

    @Test
    void immutableNotBlank_conElementoEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> ValueObjects.immutableNotBlank(java.util.Arrays.asList("a", "  "), "campo"));
    }

    // ------------------------------------------------------------------
    // immutableNonNull
    // ------------------------------------------------------------------

    @Test
    void immutableNonNull_deberiaDevolverListaInmutable() {
        var result = ValueObjects.immutableNonNull(List.of(1, 2, 3), "campo");
        assertEquals(List.of(1, 2, 3), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(4));
    }

    @Test
    void immutableNonNull_conNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.immutableNonNull(null, "campo"));
    }

    @Test
    void immutableNonNull_conElementoNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> ValueObjects.immutableNonNull(java.util.Arrays.asList("a", null), "campo"));
    }

    // ------------------------------------------------------------------
    // requireInRange (int y double)
    // ------------------------------------------------------------------

    @Test
    void requireInRange_int_deberiaDevolverElValorEnRango() {
        assertEquals(5, ValueObjects.requireInRange(5, 0, 100, "campo"));
    }

    @Test
    void requireInRange_int_limiteInferior_deberiaAceptarse() {
        assertEquals(0, ValueObjects.requireInRange(0, 0, 100, "campo"));
    }

    @Test
    void requireInRange_int_limiteSuperior_deberiaAceptarse() {
        assertEquals(100, ValueObjects.requireInRange(100, 0, 100, "campo"));
    }

    @Test
    void requireInRange_int_fueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.requireInRange(101, 0, 100, "campo"));
    }

    @Test
    void requireInRange_double_deberiaDevolverElValorEnRango() {
        assertEquals(0.5, ValueObjects.requireInRange(0.5, 0.0, 1.0, "campo"));
    }

    @Test
    void requireInRange_double_fueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.requireInRange(1.1, 0.0, 1.0, "campo"));
    }

    // ------------------------------------------------------------------
    // requireNonNegative (int y double)
    // ------------------------------------------------------------------

    @Test
    void requireNonNegative_int_deberiaDevolverElValor() {
        assertEquals(3, ValueObjects.requireNonNegative(3, "campo"));
    }

    @Test
    void requireNonNegative_int_negativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.requireNonNegative(-1, "campo"));
    }

    @Test
    void requireNonNegative_double_deberiaDevolverElValor() {
        assertEquals(2.5, ValueObjects.requireNonNegative(2.5, "campo"));
    }

    @Test
    void requireNonNegative_double_negativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ValueObjects.requireNonNegative(-0.5, "campo"));
    }
}
