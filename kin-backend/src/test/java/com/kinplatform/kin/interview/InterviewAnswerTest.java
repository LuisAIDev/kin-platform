package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewAnswerTest {

    @Test
    void of_deberiaExponerCampos() {
        var answer = InterviewAnswer.of("q1", "Modelo de suscripción mensual");

        assertEquals("q1", answer.questionId());
        assertEquals("Modelo de suscripción mensual", answer.content());
        assertTrue(answer.hasContent());
    }

    @Test
    void constructor_deberiaAceptarContenidoVacio() {
        var answer = new InterviewAnswer("q1", "");

        assertEquals("", answer.content());
        assertFalse(answer.hasContent());
    }

    @Test
    void constructor_deberiaNormalizarContenidoNulo() {
        var answer = new InterviewAnswer("q1", null);

        assertEquals("", answer.content());
        assertFalse(answer.hasContent());
    }

    @Test
    void constructor_deberiaValidarQuestionId() {
        assertThrows(IllegalArgumentException.class, () -> new InterviewAnswer(null, "contenido"));
        assertThrows(IllegalArgumentException.class, () -> new InterviewAnswer(" ", "contenido"));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewAnswer.of("q1", "contenido");
        var b = InterviewAnswer.of("q1", "contenido");
        var c = InterviewAnswer.of("q1", "otro");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("questionId=q1"));
    }
}
