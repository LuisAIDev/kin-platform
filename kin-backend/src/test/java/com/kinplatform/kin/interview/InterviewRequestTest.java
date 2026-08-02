package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewRequestTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static InterviewContext context() {
        return InterviewContext.ofProject(PROJECT_ID);
    }

    private static InterviewState state() {
        return InterviewState.empty(PROJECT_ID);
    }

    @Test
    void of_deberiaExponerCampos() {
        var context = context();
        var answer = InterviewAnswer.of("q1", "respuesta");
        var previous = state();
        var request = InterviewRequest.of(context, answer, previous);

        assertEquals(context, request.context());
        assertEquals(answer, request.answer());
        assertEquals(previous, request.previousState());
        assertTrue(request.hasAnswer());
    }

    @Test
    void of_conRespuestaNula_deberiaPermitirPrimerTurno() {
        var request = InterviewRequest.of(context(), null, state());

        assertNull(request.answer());
        assertFalse(request.hasAnswer());
    }

    @Test
    void constructor_deberiaValidarContext() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewRequest(null, null, state()));
    }

    @Test
    void constructor_deberiaValidarEstadoPrevio() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewRequest(context(), null, null));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewRequest.of(context(), null, state());
        var b = InterviewRequest.of(context(), null, state());
        var c = InterviewRequest.of(context(), InterviewAnswer.of("q1", "x"), state());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("context="));
    }
}
