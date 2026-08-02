package com.kinplatform.kin.interview.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewContext;
import com.kinplatform.kin.interview.InterviewDecision;
import com.kinplatform.kin.interview.InterviewInput;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewRequest;
import com.kinplatform.kin.interview.InterviewState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewEngineTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static InterviewContext context() {
        return InterviewContext.ofProject(PROJECT_ID);
    }

    private static InterviewBlueprint blueprint() {
        return new InterviewBlueprint(List.of(
            InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR, "sector del negocio", 1),
            InterviewQuestion.required("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", 2),
            InterviewQuestion.optional("q-city", AnalyzedDimension.CITY, "ubicación", 3)));
    }

    private static InterviewEngine engine() {
        return new InterviewEngine(blueprint(), new AnswerValidator());
    }

    private static InterviewRequest request(InterviewState state, InterviewAnswer answer) {
        return InterviewRequest.of(context(), answer, state);
    }

    private static InterviewAnswer answer(String questionId, String content) {
        return InterviewAnswer.of(questionId, content);
    }

    @Test
    void primerTurno_deberiaPreguntarLaPrimeraPregunta() {
        var engine = engine();
        var result = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));

        assertTrue(result.decision().isAsk());
        assertNotNull(result.directive());
        assertEquals("q-sector", result.directive().questionId());
        assertEquals(AnalyzedDimension.SECTOR, result.directive().dimension());
        assertEquals("q-sector", result.state().current());
        assertFalse(result.complete());
        assertEquals(1, result.state().exchangeUsed());
        assertEquals("q-sector", result.state().pending().get(0));
        assertEquals(List.of("q-sector", "q-revenue", "q-city"), result.state().pending());
    }

    @Test
    void respuestaValida_deberiaAvanzarAlSiguientePregunta() {
        var engine = engine();
        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        var second = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-sector", "Retail de alimentos en Bogotá")), "Retail"));

        assertTrue(second.decision().isAsk());
        assertEquals("q-revenue", second.directive().questionId());
        assertTrue(second.state().hasAnswered("q-sector"));
        assertEquals(2, second.state().exchangeUsed());
    }

    @Test
    void respuestaInvalida_deberiaRefinarLaMismaPregunta() {
        var strict = new InterviewBlueprint(List.of(
            new InterviewQuestion("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1,
                AnswerRules.of(10, true, 1), List.of())));
        var engine = new InterviewEngine(strict, new AnswerValidator());

        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        var refined = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-sector", "corto")), "corto"));

        assertTrue(refined.decision().isAsk());
        assertEquals("q-sector", refined.state().current());
        assertEquals(1, refined.state().refinements().get("q-sector"));
        assertFalse(refined.state().hasAnswered("q-sector"));

        var exhausted = engine.evaluate(InterviewInput.of(
            request(refined.state(), answer("q-sector", "abc")), "abc"));

        assertEquals("q-sector", exhausted.state().current());
        assertEquals(1, exhausted.state().refinements().get("q-sector"));
    }

    @Test
    void respuestaRechazada_deberiaMantenerLaPregunta() {
        var strict = new InterviewBlueprint(List.of(
            new InterviewQuestion("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1,
                AnswerRules.of(10, false, 0), List.of())));
        var engine = new InterviewEngine(strict, new AnswerValidator());

        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        var rejected = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-sector", "corto")), "corto"));

        assertEquals("q-sector", rejected.state().current());
        assertFalse(rejected.state().hasAnswered("q-sector"));
        assertTrue(rejected.state().refinements().isEmpty());
        assertTrue(rejected.decision().isAsk());
    }

    @Test
    void respuestaDeOtraPregunta_deberiaIgnorarse() {
        var engine = engine();
        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        var outOfOrder = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-revenue", "suscripción")), "suscripción"));

        assertEquals("q-sector", outOfOrder.state().current());
        assertFalse(outOfOrder.state().hasAnswered("q-revenue"));
    }

    @Test
    void todasLasObligatorias_deberianCompletarLaEntrevista() {
        var engine = engine();
        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        var second = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-sector", "Retail de alimentos")), "a"));
        var third = engine.evaluate(InterviewInput.of(
            request(second.state(), answer("q-revenue", "Suscripción mensual por usuario")), "b"));

        assertTrue(third.decision().isReport());
        assertNull(third.directive());
        assertTrue(third.complete());
        assertTrue(third.state().isComplete());
        assertTrue(third.state().hasAnswered("q-sector"));
        assertTrue(third.state().hasAnswered("q-revenue"));
        assertNull(third.state().current());
        assertTrue(third.state().pending().isEmpty());
        assertEquals(2, third.state().answeredCount());
    }

    @Test
    void presupuestoAgotado_deberiaCompletarLaEntrevista() {
        var engine = engine();
        var first = engine.evaluate(InterviewInput.of(
            request(InterviewState.empty(PROJECT_ID, 1), null), ""));
        var second = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-sector", "Retail de alimentos")), "a"));

        assertTrue(second.decision().isReport());
        assertTrue(second.complete());
        assertEquals(1, second.state().answeredCount());
    }

    @Test
    void estadoCompletoPrevio_deberiaNoProcesar() {
        var engine = engine();
        var complete = InterviewState.empty(PROJECT_ID).withComplete(true);
        var result = engine.evaluate(InterviewInput.of(request(complete, answer("q-sector", "a")), "a"));

        assertTrue(result.decision().isReport());
        assertTrue(result.complete());
        assertFalse(result.state().hasAnswered("q-sector"));
    }

    @Test
    void inputNulo_oMotorSinDependencias_deberiaDegradarAVacio() {
        var engine = engine();

        assertTrue(engine.evaluate(null).isEmpty());

        var emptyEngine = new InterviewEngine(null, null);
        assertTrue(emptyEngine.evaluate(
            InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), "")).isEmpty());

        var noValidator = new InterviewEngine(blueprint(), null);
        assertTrue(noValidator.evaluate(
            InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), "")).isEmpty());
    }

    @Test
    void preguntaActualInexistente_deberiaIgnorarLaRespuesta() {
        var stray = InterviewState.empty(PROJECT_ID).withCurrent("q-inexistente");
        var engine = engine();
        var result = engine.evaluate(InterviewInput.of(
            request(stray, answer("q-inexistente", "respuesta huérfana")), "respuesta"));

        assertFalse(result.state().hasAnswered("q-inexistente"));
        assertEquals("q-sector", result.state().current());
        assertFalse(result.complete());
    }

    @Test
    void confianza_deberiaReflejarElProgreso() {
        var engine = engine();
        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        var second = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-sector", "Retail de alimentos")), "a"));

        assertEquals(1.0 / 3.0, second.confidence(), 0.0001);
        assertEquals(2, second.state().exchangeUsed());
    }

    @Test
    void primerTurnoConRespuestaPrevia_deberiaIgnorarla() {
        var engine = engine();
        var result = engine.evaluate(InterviewInput.of(
            request(InterviewState.empty(PROJECT_ID), answer("q-sector", "respuesta adelantada")), "x"));

        assertEquals("q-sector", result.state().current());
        assertFalse(result.state().hasAnswered("q-sector"));
    }

    @Test
    void followUps_deberianPreguntarseTrasElPadre() {
        var parent = new InterviewQuestion("q-revenue", AnalyzedDimension.REVENUE_MODEL,
            "modelo de ingresos", true, 1, AnswerRules.defaults(), List.of("q-det"));
        var det = InterviewQuestion.optional("q-det", AnalyzedDimension.REVENUE_MODEL,
            "detalle del modelo", 2);
        var extra = InterviewQuestion.required("q-extra", AnalyzedDimension.SECTOR, "pendiente", 3);
        var engine = new InterviewEngine(new InterviewBlueprint(List.of(parent, det, extra)), new AnswerValidator());

        var first = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));
        assertEquals("q-revenue", first.directive().questionId());

        var second = engine.evaluate(InterviewInput.of(
            request(first.state(), answer("q-revenue", "Suscripción mensual")), "a"));

        assertEquals("q-det", second.directive().questionId());
        assertTrue(second.state().hasAnswered("q-revenue"));
    }

    @Test
    void metadata_deberiaExponerElMotor() {
        var metadata = engine().metadata();

        assertEquals(InterviewEngine.GENERATOR_NAME, metadata.name());
        assertEquals(InterviewEngine.ENGINE_VERSION, metadata.version());
        assertEquals(EnginePhase.VALIDATION, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals(40, metadata.priority());
    }

    @Test
    void resultado_deberiaRegistrarTrazabilidad() {
        var engine = engine();
        var result = engine.evaluate(InterviewInput.of(request(InterviewState.empty(PROJECT_ID), null), ""));

        assertEquals(InterviewEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(InterviewEngine.ENGINE_VERSION, result.engineVersion());
        assertEquals(InterviewDecision.Action.ASK, result.decision().action());
        assertFalse(result.isEmpty());
    }
}
