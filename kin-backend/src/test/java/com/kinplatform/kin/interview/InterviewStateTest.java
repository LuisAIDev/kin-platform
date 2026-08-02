package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewStateTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static InterviewAnswer answer(String questionId) {
        return InterviewAnswer.of(questionId, "respuesta de " + questionId);
    }

    @Test
    void empty_deberiaCrearEstadoPorDefecto() {
        var state = InterviewState.empty(PROJECT_ID);

        assertEquals(PROJECT_ID, state.projectId());
        assertTrue(state.answered().isEmpty());
        assertTrue(state.pending().isEmpty());
        assertNull(state.current());
        assertTrue(state.refinements().isEmpty());
        assertFalse(state.complete());
        assertEquals(InterviewState.DEFAULT_EXCHANGE_BUDGET, state.exchangeBudget());
        assertEquals(0, state.exchangeUsed());
        assertEquals(0, state.answeredCount());
        assertEquals(0, state.pendingCount());
        assertFalse(state.isComplete());
        assertFalse(state.hasPendingQuestions());
        assertEquals(Optional.empty(), state.currentQuestionId());
    }

    @Test
    void empty_conPresupuesto_deberiaRespetarlo() {
        assertEquals(0, InterviewState.empty(PROJECT_ID, 0).exchangeBudget());
        assertEquals(5, InterviewState.empty(PROJECT_ID, 5).exchangeBudget());
    }

    @Test
    void restore_deberiaReconstruirElEstado() {
        var answered = Map.of("q1", answer("q1"));
        var refinements = Map.of("q1", 2);
        var state = InterviewState.restore(PROJECT_ID, answered, List.of("q2"), "q2",
            refinements, false, 20, 7);

        assertEquals(answered, state.answered());
        assertEquals(List.of("q2"), state.pending());
        assertEquals("q2", state.current());
        assertEquals(refinements, state.refinements());
        assertEquals(20, state.exchangeBudget());
        assertEquals(7, state.exchangeUsed());
        assertFalse(state.complete());
        assertTrue(state.hasAnswered("q1"));
        assertTrue(state.hasPendingQuestions());
        assertEquals(Optional.of("q2"), state.currentQuestionId());
    }

    @Test
    void constructor_deberiaValidarProjectId() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewState(null, Map.of(), List.of(), null, Map.of(), false, 20, 0));
    }

    @Test
    void constructor_deberiaNormalizarYAcotar() {
        var state = new InterviewState(PROJECT_ID, null, null, " ", null, false, -3, -1);

        assertTrue(state.answered().isEmpty());
        assertTrue(state.pending().isEmpty());
        assertNull(state.current());
        assertTrue(state.refinements().isEmpty());
        assertEquals(0, state.exchangeBudget());
        assertEquals(0, state.exchangeUsed());
    }

    @Test
    void constructor_deberiaProtegerColecciones() {
        var answered = new HashMap<>(Map.of("q1", answer("q1")));
        var pending = new ArrayList<>(List.of("q2"));
        var refinements = new HashMap<>(Map.of("q1", 1));
        var state = new InterviewState(PROJECT_ID, answered, pending, "q1", refinements, false, 20, 0);

        answered.put("q2", answer("q2"));
        pending.add("q3");
        refinements.put("q2", 3);

        assertEquals(1, state.answered().size());
        assertEquals(1, state.pending().size());
        assertEquals(1, state.refinements().size());
        assertThrows(UnsupportedOperationException.class,
            () -> state.answered().put("x", answer("x")));
        assertThrows(UnsupportedOperationException.class,
            () -> state.pending().add("x"));
        assertThrows(UnsupportedOperationException.class,
            () -> state.refinements().put("x", 1));
    }

    @Test
    void withAnswered_deberiaDevolverCopiaIndependiente() {
        var original = InterviewState.empty(PROJECT_ID);
        var updated = original.withAnswered(Map.of("q1", answer("q1")));

        assertNotSame(original, updated);
        assertTrue(original.answered().isEmpty());
        assertEquals(1, updated.answeredCount());
        assertEquals(original.exchangeBudget(), updated.exchangeBudget());
    }

    @Test
    void withPending_deberiaDevolverCopiaIndependiente() {
        var original = InterviewState.empty(PROJECT_ID);
        var updated = original.withPending(List.of("q2", "q3"));

        assertNotSame(original, updated);
        assertTrue(original.pending().isEmpty());
        assertEquals(2, updated.pendingCount());
    }

    @Test
    void withCurrent_deberiaActualizarSoloLaPreguntaActual() {
        var original = InterviewState.empty(PROJECT_ID);
        var updated = original.withCurrent("q1");

        assertNotSame(original, updated);
        assertNull(original.current());
        assertEquals("q1", updated.current());
        assertEquals(Optional.of("q1"), updated.currentQuestionId());
    }

    @Test
    void withRefinements_deberiaActualizarRefinamientos() {
        var original = InterviewState.empty(PROJECT_ID);
        var updated = original.withRefinements(Map.of("q1", 2));

        assertNotSame(original, updated);
        assertTrue(original.refinements().isEmpty());
        assertEquals(2, updated.refinements().get("q1"));
    }

    @Test
    void withComplete_deberiaMarcarCompletitud() {
        var original = InterviewState.empty(PROJECT_ID);
        var updated = original.withComplete(true);

        assertNotSame(original, updated);
        assertFalse(original.isComplete());
        assertTrue(updated.isComplete());
    }

    @Test
    void withExchangeUsed_deberiaAcotarElUso() {
        var original = InterviewState.empty(PROJECT_ID, 20);
        var updated = original.withExchangeUsed(3);

        assertEquals(3, updated.exchangeUsed());
        assertEquals(20, updated.exchangeBudget());
    }

    @Test
    void remainingBudget_deberiaCalcularElPresupuestoRestante() {
        var state = InterviewState.empty(PROJECT_ID, 20).withExchangeUsed(7);

        assertEquals(13, state.remainingBudget());
    }

    @Test
    void remainingBudget_presupuestoIlimitado_deberiaSerMaximo() {
        assertEquals(Integer.MAX_VALUE, InterviewState.empty(PROJECT_ID, 0).remainingBudget());
    }

    @Test
    void remainingBudget_excedido_deberiaAcotarACero() {
        assertEquals(0, InterviewState.empty(PROJECT_ID, 20).withExchangeUsed(25).remainingBudget());
    }

    @Test
    void toProgress_deberiaProyectarElProgreso() {
        var state = InterviewState.empty(PROJECT_ID, 20)
            .withAnswered(Map.of("q1", answer("q1"), "q2", answer("q2")))
            .withPending(List.of("q3", "q4", "q5"))
            .withExchangeUsed(2);

        var progress = state.toProgress(5);

        assertEquals(2, progress.answeredCount());
        assertEquals(3, progress.pendingCount());
        assertEquals(5, progress.totalQuestions());
        assertEquals(2, progress.exchangeUsed());
        assertEquals(20, progress.exchangeBudget());
        assertEquals(0.4, progress.completenessRatio(), 0.0001);
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewState.empty(PROJECT_ID);
        var b = InterviewState.empty(PROJECT_ID);
        var c = InterviewState.empty(PROJECT_ID, 30);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("projectId="));
    }
}
