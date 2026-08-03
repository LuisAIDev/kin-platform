package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketPlanTest {

    @Test
    void empty_deberiaCrearPlanConMercadosEnCero() {
        var plan = MarketPlan.empty();

        assertEquals(0.0, plan.tam());
        assertEquals(0.0, plan.sam());
        assertEquals(0.0, plan.som());
        assertEquals(0.0, plan.growthRate());
        assertTrue(plan.competitors().isEmpty());
        assertTrue(plan.channels().isEmpty());
        assertTrue(plan.entryBarriers().isEmpty());
        assertTrue(plan.customerSegments().isEmpty());
        assertEquals(0.0, plan.confidence());
    }

    @Test
    void of_deberiaGuardarTodosLosCampos() {
        var plan = MarketPlan.of(1_000_000, 500_000, 100_000, 12.5,
            List.of("Competidor A"), List.of("Directo"), List.of("Regulación"),
            List.of("PYME"), 0.8);

        assertEquals(1_000_000, plan.tam());
        assertEquals(500_000, plan.sam());
        assertEquals(100_000, plan.som());
        assertEquals(12.5, plan.growthRate());
        assertEquals(List.of("Competidor A"), plan.competitors());
        assertEquals(List.of("Directo"), plan.channels());
        assertEquals(List.of("Regulación"), plan.entryBarriers());
        assertEquals(List.of("PYME"), plan.customerSegments());
        assertEquals(0.8, plan.confidence());
    }

    @Test
    void of_conTamNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(-1, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), 0.5));
    }

    @Test
    void of_conSamNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, -1, 0, 0, List.of(), List.of(), List.of(), List.of(), 0.5));
    }

    @Test
    void of_conSomNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, 0, -1, 0, List.of(), List.of(), List.of(), List.of(), 0.5));
    }

    @Test
    void of_conGrowthRateNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, 0, 0, -1, List.of(), List.of(), List.of(), List.of(), 0.5));
    }

    @Test
    void of_conConfidenceFueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), 1.1));
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), -0.1));
    }

    @Test
    void of_conListaConBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, 0, 0, 0, List.of(""), List.of(), List.of(), List.of(), 0.5));
    }

    @Test
    void of_conListaNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> MarketPlan.of(0, 0, 0, 0, null, List.of(), List.of(), List.of(), 0.5));
    }

    @Test
    void listas_deberianSerInmutables() {
        var plan = MarketPlan.of(0, 0, 0, 0, List.of("A"), List.of(), List.of(), List.of(), 0.5);
        assertThrows(UnsupportedOperationException.class, () -> plan.competitors().add("B"));
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var a = MarketPlan.of(1, 2, 3, 4, List.of(), List.of(), List.of(), List.of(), 0.5);
        var b = MarketPlan.of(1, 2, 3, 4, List.of(), List.of(), List.of(), List.of(), 0.5);
        var c = MarketPlan.of(9, 2, 3, 4, List.of(), List.of(), List.of(), List.of(), 0.5);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "plan");
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        assertNotNull(MarketPlan.empty().toString());
        assertTrue(MarketPlan.empty().toString().contains("tam"));
    }
}
