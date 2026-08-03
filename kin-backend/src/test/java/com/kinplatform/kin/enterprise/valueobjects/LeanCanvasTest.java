package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeanCanvasTest {

    @Test
    void empty_deberiaCrearCanvasConNueveBloquesVacios() {
        var canvas = LeanCanvas.empty();

        assertTrue(canvas.problem().isEmpty());
        assertTrue(canvas.customerSegments().isEmpty());
        assertTrue(canvas.uniqueValueProposition().isEmpty());
        assertTrue(canvas.solution().isEmpty());
        assertTrue(canvas.channels().isEmpty());
        assertTrue(canvas.revenueStreams().isEmpty());
        assertTrue(canvas.costStructure().isEmpty());
        assertTrue(canvas.keyMetrics().isEmpty());
        assertTrue(canvas.unfairAdvantage().isEmpty());
    }

    @Test
    void of_deberiaCrearCanvasConLosNueveBloques() {
        var canvas = LeanCanvas.of(
            List.of("Problema 1"), List.of("Segmento 1"),
            List.of("Propuesta"), List.of("Solución"),
            List.of("Canal"), List.of("Ingresos"),
            List.of("Coste"), List.of("Métrica"),
            List.of("Ventaja"));

        assertEquals(List.of("Problema 1"), canvas.problem());
        assertEquals(List.of("Segmento 1"), canvas.customerSegments());
        assertEquals(List.of("Propuesta"), canvas.uniqueValueProposition());
        assertEquals(List.of("Solución"), canvas.solution());
        assertEquals(List.of("Canal"), canvas.channels());
        assertEquals(List.of("Ingresos"), canvas.revenueStreams());
        assertEquals(List.of("Coste"), canvas.costStructure());
        assertEquals(List.of("Métrica"), canvas.keyMetrics());
        assertEquals(List.of("Ventaja"), canvas.unfairAdvantage());
    }

    @Test
    void bloques_deberianSerInmutables() {
        var canvas = LeanCanvas.of(List.of("Problema 1"), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of());

        assertThrows(UnsupportedOperationException.class, () -> canvas.problem().add("Otro"));
    }

    @Test
    void of_conBloqueNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> LeanCanvas.of(null, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void of_conElementoEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> LeanCanvas.of(List.of(""), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void of_conElementoNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> LeanCanvas.of(java.util.Arrays.asList("a", null), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var a = LeanCanvas.of(List.of("P1"), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of());
        var b = LeanCanvas.of(List.of("P1"), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of());
        var c = LeanCanvas.of(List.of("P2"), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "canvas");
    }

    @Test
    void toString_deberiaIncluirLosBloques() {
        var canvas = LeanCanvas.empty();
        var text = canvas.toString();

        assertNotNull(text);
        assertTrue(text.contains("problem"));
    }
}
