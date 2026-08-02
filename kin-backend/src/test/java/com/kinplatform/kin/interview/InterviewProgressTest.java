package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewProgressTest {

    @Test
    void of_deberiaExponerCampos() {
        var progress = InterviewProgress.of(3, 5, 8, 3, 20, false);

        assertEquals(3, progress.answeredCount());
        assertEquals(5, progress.pendingCount());
        assertEquals(8, progress.totalQuestions());
        assertEquals(3, progress.exchangeUsed());
        assertEquals(20, progress.exchangeBudget());
        assertFalse(progress.complete());
    }

    @Test
    void empty_deberiaEstarVacia() {
        var progress = InterviewProgress.empty();

        assertEquals(0, progress.answeredCount());
        assertEquals(0, progress.pendingCount());
        assertEquals(0, progress.totalQuestions());
        assertEquals(0, progress.exchangeUsed());
        assertEquals(0, progress.exchangeBudget());
        assertFalse(progress.complete());
        assertEquals(0.0, progress.completenessRatio(), 0.0001);
    }

    @Test
    void constructor_deberiaAcotarContadoresNegativos() {
        var progress = new InterviewProgress(-1, -2, -3, -4, -5, false);

        assertEquals(0, progress.answeredCount());
        assertEquals(0, progress.pendingCount());
        assertEquals(0, progress.totalQuestions());
        assertEquals(0, progress.exchangeUsed());
        assertEquals(0, progress.exchangeBudget());
    }

    @Test
    void completenessRatio_deberiaCalcularCobertura() {
        var progress = InterviewProgress.of(4, 4, 8, 0, 20, false);

        assertEquals(0.5, progress.completenessRatio(), 0.0001);
    }

    @Test
    void completenessRatio_deberiaAcotarAlCienPorCiento() {
        var progress = InterviewProgress.of(10, 0, 8, 0, 20, true);

        assertEquals(1.0, progress.completenessRatio(), 0.0001);
    }

    @Test
    void completenessRatio_sinPreguntas_deberiaSerCero() {
        assertEquals(0.0, InterviewProgress.empty().completenessRatio(), 0.0001);
    }

    @Test
    void remainingBudget_deberiaCalcularPresupuestoRestante() {
        assertEquals(17, InterviewProgress.of(0, 0, 0, 3, 20, false).remainingBudget());
    }

    @Test
    void remainingBudget_presupuestoIlimitado_deberiaSerMaximo() {
        assertEquals(Integer.MAX_VALUE, InterviewProgress.of(0, 0, 0, 3, 0, false).remainingBudget());
    }

    @Test
    void remainingBudget_deberiaAcotarACero() {
        assertEquals(0, InterviewProgress.of(0, 0, 0, 25, 20, false).remainingBudget());
    }

    @Test
    void from_deberiaDerivarseDelEstado() {
        var projectId = UUID.randomUUID();
        var state = InterviewState.empty(projectId, 10);
        var progress = InterviewProgress.from(state, 6);

        assertEquals(0, progress.answeredCount());
        assertEquals(0, progress.pendingCount());
        assertEquals(6, progress.totalQuestions());
        assertEquals(10, progress.exchangeBudget());
        assertEquals(0, progress.exchangeUsed());
        assertFalse(progress.complete());
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewProgress.of(3, 5, 8, 3, 20, false);
        var b = InterviewProgress.of(3, 5, 8, 3, 20, false);
        var c = InterviewProgress.of(4, 5, 8, 3, 20, false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("answeredCount=3"));
    }
}
