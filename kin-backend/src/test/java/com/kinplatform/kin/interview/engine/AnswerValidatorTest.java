package com.kinplatform.kin.interview.engine;

import com.kinplatform.kin.interview.AnswerRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerValidatorTest {

    private final AnswerValidator validator = new AnswerValidator();

    @Test
    void respuestaValida_deberiaAceptarse() {
        var validation = validator.validate("Nuestra solución es una app móvil", null);

        assertTrue(validation.isAccepted());
        assertTrue(validation.accepted());
        assertFalse(validation.requiresRefinement());
        assertEquals("", validation.reason());
    }

    @Test
    void vacia_deberiaSolicitarRefinamiento() {
        var validation = validator.validate("", AnswerRules.defaults());

        assertFalse(validation.accepted());
        assertTrue(validation.requiresRefinement());
        assertEquals(1, validation.refinementCount());
    }

    @Test
    void blanco_deberiaSolicitarRefinamiento() {
        var validation = validator.validate("   ", AnswerRules.defaults());

        assertFalse(validation.accepted());
        assertTrue(validation.requiresRefinement());
    }

    @Test
    void longitudMinima_deberiaRefinarYAcotarRefinamientos() {
        var rules = AnswerRules.of(10, true, 1);
        var first = validator.validate("corto", rules);
        var second = validator.validate("corto", rules, 1);

        assertFalse(first.accepted());
        assertTrue(first.requiresRefinement());
        assertEquals(1, first.refinementCount());

        assertFalse(second.accepted());
        assertFalse(second.requiresRefinement());
        assertTrue(second.reason().contains("breve"));
    }

    @Test
    void sinRefinamientoPermitido_deberiaRechazar() {
        var rules = AnswerRules.of(10, false, 0);
        var validation = validator.validate("corto", rules);

        assertFalse(validation.accepted());
        assertFalse(validation.requiresRefinement());
        assertTrue(validation.reason().contains("breve"));
    }

    @Test
    void trim_deberiaAplicarseAntesDeMedir() {
        var rules = AnswerRules.of(5, false, 0);
        var validation = validator.validate("   holaa   ", rules);

        assertTrue(validation.accepted());
    }

    @Test
    void formatoRegex_deberiaValidar() {
        var rules = new AnswerRules(0, List.of(), "\\d+", false, 0);

        assertTrue(validator.validate("12345", rules).accepted());
        assertFalse(validator.validate("abc", rules).accepted());
    }

    @Test
    void formatoRegexInvalido_deberiaIgnorarse() {
        var rules = new AnswerRules(0, List.of(), "[[[", false, 0);

        assertTrue(validator.validate("cualquier cosa", rules).accepted());
    }

    @Test
    void keywords_deberianExigirseCaseInsensitive() {
        var rules = new AnswerRules(0, List.of("Clientes", "Colombia"), "", false, 0);

        assertTrue(validator.validate("Nuestros CLIENTES están en colombia", rules).accepted());
        assertFalse(validator.validate("No hablamos de eso", rules).accepted());
    }

    @Test
    void keywordEnBlanco_deberiaRechazar() {
        var rules = new AnswerRules(0, java.util.Arrays.asList("clientes", "  "), "", false, 0);

        assertFalse(validator.validate("clientes objetivo", rules).accepted());
    }

    @Test
    void reglasNulas_deberianUsarDefaults() {
        var validation = validator.validate("respuesta", null);

        assertTrue(validation.accepted());
        assertFalse(validation.requiresRefinement());
    }

    @Test
    void presupuestoDeRefinamientos_deberiaAcotarse() {
        var rules = AnswerRules.of(5, true, 2);

        assertEquals(1, validator.validate("a", rules, 0).refinementCount());
        assertEquals(2, validator.validate("a", rules, 1).refinementCount());
        var exhausted = validator.validate("a", rules, 2);

        assertFalse(exhausted.accepted());
        assertFalse(exhausted.requiresRefinement());
    }

    @Test
    void validateConDosArgumentos_deberiaPartirDeCero() {
        var rules = AnswerRules.of(10, true, 1);
        var validation = validator.validate("corto", rules);

        assertEquals(1, validation.refinementCount());
        assertTrue(validation.requiresRefinement());
    }
}
