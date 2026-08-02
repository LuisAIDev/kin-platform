package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewDecisionTest {

    @Test
    void ask_deberiaSerPregunta() {
        var decision = InterviewDecision.ask("q1", "Falta el modelo de ingresos");

        assertEquals(InterviewDecision.Action.ASK, decision.action());
        assertEquals("q1", decision.questionId());
        assertEquals("Falta el modelo de ingresos", decision.reason());
        assertTrue(decision.isAsk());
        assertFalse(decision.isReport());
    }

    @Test
    void report_deberiaSerReporte() {
        var decision = InterviewDecision.report("Entrevista completa");

        assertEquals(InterviewDecision.Action.REPORT, decision.action());
        assertNull(decision.questionId());
        assertEquals("Entrevista completa", decision.reason());
        assertTrue(decision.isReport());
        assertFalse(decision.isAsk());
    }

    @Test
    void constructor_deberiaValidarAction() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewDecision(null, "q1", "razón"));
    }

    @Test
    void constructor_deberiaNormalizarNulosYVacios() {
        var decision = new InterviewDecision(InterviewDecision.Action.ASK, " ", null);

        assertNull(decision.questionId());
        assertEquals("", decision.reason());
    }

    @Test
    void action_deberiaExponerTodasLasAcciones() {
        assertEquals(2, InterviewDecision.Action.values().length);
        assertEquals(InterviewDecision.Action.ASK, InterviewDecision.Action.valueOf("ASK"));
        assertEquals(InterviewDecision.Action.REPORT, InterviewDecision.Action.valueOf("REPORT"));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewDecision.ask("q1", "razón");
        var b = InterviewDecision.ask("q1", "razón");
        var c = InterviewDecision.report("razón");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("action=ASK"));
    }
}
