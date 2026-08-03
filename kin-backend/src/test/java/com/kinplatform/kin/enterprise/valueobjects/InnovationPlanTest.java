package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InnovationPlanTest {

    @Test
    void empty_deberiaCrearPlanConNivelIncremental() {
        var plan = InnovationPlan.empty();

        assertEquals(InnovationLevel.INCREMENTAL, plan.innovationLevel());
        assertTrue(plan.differentiators().isEmpty());
        assertEquals("Sin definir", plan.defensibility());
        assertTrue(plan.innovationRoadmap().isEmpty());
        assertTrue(plan.researchRecommendations().isEmpty());
    }

    @Test
    void of_deberiaGuardarTodosLosCampos() {
        var plan = InnovationPlan.of(InnovationLevel.DISRUPTIVE,
            List.of("Diferencial 1"), "Patentes", List.of("Fase I+D"),
            List.of("Investigar X"));

        assertEquals(InnovationLevel.DISRUPTIVE, plan.innovationLevel());
        assertEquals(List.of("Diferencial 1"), plan.differentiators());
        assertEquals("Patentes", plan.defensibility());
        assertEquals(List.of("Fase I+D"), plan.innovationRoadmap());
        assertEquals(List.of("Investigar X"), plan.researchRecommendations());
    }

    @Test
    void of_conNivelNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> InnovationPlan.of(null, List.of(), "X", List.of(), List.of()));
    }

    @Test
    void of_conDefensibilidadEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> InnovationPlan.of(InnovationLevel.INCREMENTAL, List.of(), "  ", List.of(), List.of()));
    }

    @Test
    void of_conListaConBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> InnovationPlan.of(InnovationLevel.INCREMENTAL, List.of(""), "X", List.of(), List.of()));
    }

    @Test
    void of_conListaNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> InnovationPlan.of(InnovationLevel.INCREMENTAL, null, "X", List.of(), List.of()));
    }

    @Test
    void listas_deberianSerInmutables() {
        var plan = InnovationPlan.of(InnovationLevel.INCREMENTAL, List.of("A"), "X",
            List.of(), List.of());
        assertThrows(UnsupportedOperationException.class, () -> plan.differentiators().add("B"));
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var a = InnovationPlan.of(InnovationLevel.TRANSFORMATIONAL, List.of(), "X", List.of(), List.of());
        var b = InnovationPlan.of(InnovationLevel.TRANSFORMATIONAL, List.of(), "X", List.of(), List.of());
        var c = InnovationPlan.of(InnovationLevel.INCREMENTAL, List.of(), "X", List.of(), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "plan");
    }

    @Test
    void innovationLevel_deberiaContenerLosValores() {
        var values = InnovationLevel.values();
        assertEquals(3, values.length);
        assertEquals(InnovationLevel.TRANSFORMATIONAL, InnovationLevel.valueOf("TRANSFORMATIONAL"));
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        assertNotNull(InnovationPlan.empty().toString());
        assertTrue(InnovationPlan.empty().toString().contains("innovationLevel"));
    }
}
