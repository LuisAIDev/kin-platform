package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerValidationTest {

    @Test
    void valid_deberiaEstarAceptada() {
        var validation = AnswerValidation.valid();

        assertTrue(validation.isAccepted());
        assertTrue(validation.accepted());
        assertEquals("", validation.reason());
        assertFalse(validation.requiresRefinement());
        assertEquals(0, validation.refinementCount());
    }

    @Test
    void rejected_deberiaEstarRechazada() {
        var validation = AnswerValidation.rejected("Respuesta vacía");

        assertFalse(validation.isAccepted());
        assertFalse(validation.accepted());
        assertEquals("Respuesta vacía", validation.reason());
        assertFalse(validation.requiresRefinement());
        assertEquals(0, validation.refinementCount());
    }

    @Test
    void refinement_deberiaPedirRefinamiento() {
        var validation = AnswerValidation.refinement("Demasiado breve", 2);

        assertFalse(validation.isAccepted());
        assertFalse(validation.accepted());
        assertEquals("Demasiado breve", validation.reason());
        assertTrue(validation.requiresRefinement());
        assertEquals(2, validation.refinementCount());
    }

    @Test
    void constructor_deberiaAceptarNulosYAcotar() {
        var validation = new AnswerValidation(false, null, true, -3);

        assertEquals("", validation.reason());
        assertEquals(0, validation.refinementCount());
    }

    @Test
    void factories_deberianNormalizarReasonNulo() {
        assertEquals("", AnswerValidation.rejected(null).reason());
        assertEquals("", AnswerValidation.refinement(null, 1).reason());
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = AnswerValidation.rejected("x");
        var b = AnswerValidation.rejected("x");
        var c = AnswerValidation.valid();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("reason=x"));
    }
}
