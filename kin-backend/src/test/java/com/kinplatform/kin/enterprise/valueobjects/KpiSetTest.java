package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KpiSetTest {

    // ------------------------------------------------------------------
    // Kpi
    // ------------------------------------------------------------------

    @Test
    void kpi_deberiaGuardarValores() {
        var kpi = KpiSet.Kpi.of("CAC", 100.0, 80.0, "Gasto / Clientes", "Mensual");

        assertEquals("CAC", kpi.name());
        assertEquals(100.0, kpi.target());
        assertEquals(80.0, kpi.currentValue());
        assertEquals("Gasto / Clientes", kpi.formula());
        assertEquals("Mensual", kpi.frequency());
    }

    @Test
    void kpi_conNameEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> KpiSet.Kpi.of("", 1, 1, "F", "Mensual"));
    }

    @Test
    void kpi_conFormulaEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> KpiSet.Kpi.of("CAC", 1, 1, "", "Mensual"));
    }

    @Test
    void kpi_conFrequencyEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> KpiSet.Kpi.of("CAC", 1, 1, "F", "  "));
    }

    @Test
    void kpi_equals_deberiaCompararPorValor() {
        assertEquals(KpiSet.Kpi.of("CAC", 1, 1, "F", "M"), KpiSet.Kpi.of("CAC", 1, 1, "F", "M"));
        assertNotEquals(KpiSet.Kpi.of("CAC", 1, 1, "F", "M"), KpiSet.Kpi.of("CAC", 2, 1, "F", "M"));
        assertNotEquals(KpiSet.Kpi.of("CAC", 1, 1, "F", "M"), "kpi");
    }

    // ------------------------------------------------------------------
    // KpiSet
    // ------------------------------------------------------------------

    @Test
    void empty_deberiaCrearSetVacio() {
        var set = KpiSet.empty();

        assertTrue(set.acquisition().isEmpty());
        assertTrue(set.activation().isEmpty());
        assertTrue(set.retention().isEmpty());
        assertTrue(set.revenue().isEmpty());
        assertTrue(set.financial().isEmpty());
    }

    @Test
    void of_deberiaGuardarLasCincoFases() {
        var kpi = KpiSet.Kpi.of("CAC", 100, 80, "G/C", "Mensual");
        var set = KpiSet.of(List.of(kpi), List.of(), List.of(), List.of(), List.of());

        assertEquals(List.of(kpi), set.acquisition());
        assertTrue(set.activation().isEmpty());
        assertTrue(set.retention().isEmpty());
        assertTrue(set.revenue().isEmpty());
        assertTrue(set.financial().isEmpty());
    }

    @Test
    void of_conFaseNull_deberiaLanzar() {
        var kpi = KpiSet.Kpi.of("CAC", 100, 80, "G/C", "Mensual");
        assertThrows(IllegalArgumentException.class,
            () -> KpiSet.of(null, List.of(), List.of(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> KpiSet.of(List.of(kpi), null, List.of(), List.of(), List.of()));
    }

    @Test
    void of_conKpiNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> KpiSet.of(java.util.Arrays.asList((KpiSet.Kpi) null), List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void fases_deberianSerInmutables() {
        var kpi = KpiSet.Kpi.of("CAC", 100, 80, "G/C", "Mensual");
        var set = KpiSet.of(List.of(kpi), List.of(), List.of(), List.of(), List.of());
        assertThrows(UnsupportedOperationException.class, () -> set.acquisition().clear());
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var kpi = KpiSet.Kpi.of("CAC", 100, 80, "G/C", "Mensual");
        var a = KpiSet.of(List.of(kpi), List.of(), List.of(), List.of(), List.of());
        var b = KpiSet.of(List.of(kpi), List.of(), List.of(), List.of(), List.of());
        var c = KpiSet.empty();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "kpis");
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        assertNotNull(KpiSet.empty().toString());
        assertTrue(KpiSet.empty().toString().contains("acquisition"));
    }
}
