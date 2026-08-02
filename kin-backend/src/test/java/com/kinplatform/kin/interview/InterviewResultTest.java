package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewResultTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static InterviewState state(boolean complete) {
        return InterviewState.empty(PROJECT_ID).withComplete(complete);
    }

    private static InterviewDecision decision(InterviewDecision.Action action) {
        return action == InterviewDecision.Action.REPORT
            ? InterviewDecision.report("Entrevista completa")
            : InterviewDecision.ask("q1", "Falta modelo de ingresos");
    }

    private static InterviewDirective directive() {
        return InterviewDirective.of("q1", AnalyzedDimension.REVENUE_MODEL,
            "modelo de ingresos", AnswerRules.defaults());
    }

    @Test
    void of_deberiaExponerCampos() {
        var decision = decision(InterviewDecision.Action.ASK);
        var directive = directive();
        var state = state(false);
        var progress = InterviewProgress.of(0, 5, 5, 0, 20, false);
        var result = InterviewResult.of(decision, directive, state, progress);

        assertEquals(decision, result.decision());
        assertEquals(directive, result.directive());
        assertEquals(state, result.state());
        assertEquals(progress, result.progress());
        assertEquals(1.0, result.confidence(), 0.0001);
        assertEquals(decision.reason(), result.explanation());
        assertEquals("kin.interview", result.generatedBy());
        assertEquals(InterviewResult.VERSION, result.engineVersion());
        assertFalse(result.isEmpty());
        assertTrue(result.hasDirective());
        assertFalse(result.complete());
    }

    @Test
    void of_conReporte_deberiaEstarCompleta() {
        var state = state(true);
        var result = InterviewResult.of(decision(InterviewDecision.Action.REPORT), null, state,
            InterviewProgress.of(5, 0, 5, 5, 20, true));

        assertTrue(result.complete());
        assertFalse(result.hasDirective());
        assertTrue(result.decision().isReport());
    }

    @Test
    void of_conDecisionNula_deberiaNormalizarExplicacion() {
        var result = InterviewResult.of(null, null, state(false), InterviewProgress.empty());

        assertNull(result.decision());
        assertEquals("", result.explanation());
        assertEquals(1.0, result.confidence(), 0.0001);
        assertFalse(result.isEmpty());
    }

    @Test
    void empty_deberiaEstarVacia() {
        var result = InterviewResult.empty();

        assertTrue(result.isEmpty());
        assertNull(result.state());
        assertNull(result.directive());
        assertFalse(result.complete());
        assertTrue(result.decision().isReport());
        assertEquals(InterviewProgress.empty(), result.progress());
        assertEquals(0.0, result.confidence(), 0.0001);
        assertEquals("kin.interview", result.generatedBy());
        assertEquals(InterviewResult.VERSION, result.engineVersion());
    }

    @Test
    void constructor_deberiaNormalizarNulos() {
        var result = new InterviewResult(null, null, null, null, 5.0, null, null, null);

        assertEquals(InterviewProgress.empty(), result.progress());
        assertEquals("", result.explanation());
        assertEquals("", result.generatedBy());
        assertEquals("", result.engineVersion());
    }

    @Test
    void constructor_deberiaAcotarLaConfianza() {
        var result = new InterviewResult(null, null, null, null, 2.5, "", "", "");
        assertEquals(1.0, result.confidence(), 0.0001);

        var result2 = new InterviewResult(null, null, null, null, -1.0, "", "", "");
        assertEquals(0.0, result2.confidence(), 0.0001);
    }

    @Test
    void isEmpty_deberiaReflejarLaPresenciaDeEstado() {
        assertTrue(InterviewResult.empty().isEmpty());
        assertFalse(InterviewResult.of(decision(InterviewDecision.Action.ASK), null,
            state(false), InterviewProgress.empty()).isEmpty());
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewResult.of(decision(InterviewDecision.Action.ASK), directive(), state(false),
            InterviewProgress.empty());
        var b = InterviewResult.of(decision(InterviewDecision.Action.ASK), directive(), state(false),
            InterviewProgress.empty());
        var c = InterviewResult.empty();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("generatedBy=kin.interview"));
    }

    @Test
    void deberiaImplementarEngineResult() {
        var interfaces = InterviewResult.class.getInterfaces();
        assertEquals(1, interfaces.length);
        assertEquals("EngineResult", interfaces[0].getSimpleName());

        var result = InterviewResult.empty();
        assertEquals(0.0, result.confidence(), 0.0001);
        assertTrue(result.isEmpty());
    }

    @Test
    void conEstadoCompleto_deberiaConservarRespuestas() {
        var answers = Map.of("q1", InterviewAnswer.of("q1", "respuesta"));
        var state = InterviewState.empty(PROJECT_ID).withAnswered(answers);
        var result = InterviewResult.of(decision(InterviewDecision.Action.REPORT), null, state,
            state.toProgress(1));

        assertEquals(1, result.state().answeredCount());
        assertEquals(1.0, result.state().toProgress(1).completenessRatio(), 0.0001);
    }
}
