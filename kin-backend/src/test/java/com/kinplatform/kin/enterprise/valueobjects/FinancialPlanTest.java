package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialPlanTest {

    private static final FinancialPlan.Scenario OPTIMISTIC = new FinancialPlan.Scenario(500_000, 40);
    private static final FinancialPlan.Scenario BASE = new FinancialPlan.Scenario(300_000, 30);
    private static final FinancialPlan.Scenario PESSIMISTIC = new FinancialPlan.Scenario(100_000, 20);

    // ------------------------------------------------------------------
    // Scenario
    // ------------------------------------------------------------------

    @Test
    void scenario_deberiaGuardarValores() {
        var scenario = FinancialPlan.Scenario.of(250_000, 25.5);

        assertEquals(250_000, scenario.revenue());
        assertEquals(25.5, scenario.margin());
    }

    @Test
    void scenario_conRevenueNegativa_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> new FinancialPlan.Scenario(-1, 20));
    }

    @Test
    void scenario_conMarginFueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> new FinancialPlan.Scenario(100, 101));
        assertThrows(IllegalArgumentException.class, () -> new FinancialPlan.Scenario(100, -1));
    }

    // ------------------------------------------------------------------
    // empty
    // ------------------------------------------------------------------

    @Test
    void empty_deberiaCrearPlanConImportesEnCero() {
        var plan = FinancialPlan.empty();

        assertEquals(0.0, plan.capex());
        assertEquals(0.0, plan.opex());
        assertEquals(0.0, plan.revenueYear1());
        assertEquals(0.0, plan.revenueYear2());
        assertEquals(0.0, plan.revenueYear3());
        assertEquals(0, plan.breakEvenMonth());
        assertEquals(0.0, plan.grossMargin());
        assertEquals(0.0, plan.optimistic().revenue());
        assertEquals(0.0, plan.base().revenue());
        assertEquals(0.0, plan.pessimistic().revenue());
    }

    // ------------------------------------------------------------------
    // of
    // ------------------------------------------------------------------

    @Test
    void of_deberiaGuardarTodosLosCampos() {
        var plan = FinancialPlan.of(50_000, 20_000, 100_000, 200_000, 350_000,
            8, 45.0, OPTIMISTIC, BASE, PESSIMISTIC);

        assertEquals(50_000, plan.capex());
        assertEquals(20_000, plan.opex());
        assertEquals(100_000, plan.revenueYear1());
        assertEquals(200_000, plan.revenueYear2());
        assertEquals(350_000, plan.revenueYear3());
        assertEquals(8, plan.breakEvenMonth());
        assertEquals(45.0, plan.grossMargin());
        assertEquals(OPTIMISTIC, plan.optimistic());
        assertEquals(BASE, plan.base());
        assertEquals(PESSIMISTIC, plan.pessimistic());
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    @Test
    void of_conCapexNegativa_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(-1, 0, 0, 0, 0, 0, 0, OPTIMISTIC, BASE, PESSIMISTIC));
    }

    @Test
    void of_conOpexNegativa_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, -1, 0, 0, 0, 0, 0, OPTIMISTIC, BASE, PESSIMISTIC));
    }

    @Test
    void of_conRevenueNegativa_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, -1, 0, 0, 0, 0, OPTIMISTIC, BASE, PESSIMISTIC));
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, -1, 0, 0, 0, OPTIMISTIC, BASE, PESSIMISTIC));
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, -1, 0, 0, OPTIMISTIC, BASE, PESSIMISTIC));
    }

    @Test
    void of_conBreakEvenNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, 0, -1, 0, OPTIMISTIC, BASE, PESSIMISTIC));
    }

    @Test
    void of_conGrossMarginFueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, 0, 0, 101, OPTIMISTIC, BASE, PESSIMISTIC));
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, 0, 0, -1, OPTIMISTIC, BASE, PESSIMISTIC));
    }

    @Test
    void of_conEscenarioNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, 0, 0, 0, null, BASE, PESSIMISTIC));
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, 0, 0, 0, OPTIMISTIC, null, PESSIMISTIC));
        assertThrows(IllegalArgumentException.class,
            () -> FinancialPlan.of(0, 0, 0, 0, 0, 0, 0, OPTIMISTIC, BASE, null));
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Test
    void equals_deberiaCompararPorValor() {
        var a = FinancialPlan.of(1, 2, 3, 4, 5, 6, 7, OPTIMISTIC, BASE, PESSIMISTIC);
        var b = FinancialPlan.of(1, 2, 3, 4, 5, 6, 7, OPTIMISTIC, BASE, PESSIMISTIC);
        var c = FinancialPlan.of(9, 2, 3, 4, 5, 6, 7, OPTIMISTIC, BASE, PESSIMISTIC);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "plan");
    }

    @Test
    void equals_delScenario_deberiaCompararPorValor() {
        assertEquals(FinancialPlan.Scenario.of(1, 2), FinancialPlan.Scenario.of(1, 2));
        assertNotEquals(FinancialPlan.Scenario.of(1, 2), FinancialPlan.Scenario.of(1, 3));
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        var plan = FinancialPlan.of(1, 2, 3, 4, 5, 6, 7, OPTIMISTIC, BASE, PESSIMISTIC);
        var text = plan.toString();

        assertNotNull(text);
        assertTrue(text.contains("capex"));
    }
}
