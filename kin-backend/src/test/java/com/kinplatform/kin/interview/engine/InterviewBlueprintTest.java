package com.kinplatform.kin.interview.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewBlueprintTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static InterviewQuestion q(String id, AnalyzedDimension dimension, String topic,
                                       boolean required, int order, List<String> followUps) {
        return new InterviewQuestion(id, dimension, topic, required, order, AnswerRules.defaults(), followUps);
    }

    private static InterviewQuestion q(String id, AnalyzedDimension dimension, String topic,
                                       boolean required, int order) {
        return q(id, dimension, topic, required, order, List.of());
    }

    private static InterviewState emptyState() {
        return InterviewState.empty(PROJECT_ID);
    }

    @Test
    void next_deberiaSeleccionarLaPrimeraElegiblePorOrden() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", true, 2),
            q("q-sector", AnalyzedDimension.SECTOR, "sector del negocio", true, 1)));

        var next = blueprint.next(emptyState());

        assertTrue(next.isPresent());
        assertEquals("q-sector", next.get().id());
    }

    @Test
    void next_conMismoOrden_deberiaDesempatarPorId() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-b", AnalyzedDimension.CITY, "b", true, 1),
            q("q-a", AnalyzedDimension.SECTOR, "a", true, 1)));

        assertEquals("q-a", blueprint.next(emptyState()).get().id());
    }

    @Test
    void next_deberiaOmitirLasRespondidas() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1),
            q("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo", true, 2)));

        var state = emptyState()
            .withAnswered(java.util.Map.of("q-sector",
                com.kinplatform.kin.interview.InterviewAnswer.of("q-sector", "retail")));

        assertEquals("q-revenue", blueprint.next(state).get().id());
    }

    @Test
    void isComplete_conObligatoriasRespondidas_deberiaSerTrue() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1),
            q("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo", true, 2)));

        var state = emptyState()
            .withAnswered(java.util.Map.of(
                "q-sector", com.kinplatform.kin.interview.InterviewAnswer.of("q-sector", "a"),
                "q-revenue", com.kinplatform.kin.interview.InterviewAnswer.of("q-revenue", "b")));

        assertTrue(blueprint.isComplete(state));
    }

    @Test
    void isComplete_conOpcionalPendiente_deberiaSerTrue() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1),
            q("q-city", AnalyzedDimension.CITY, "ubicación", false, 2)));

        var state = emptyState()
            .withAnswered(java.util.Map.of("q-sector",
                com.kinplatform.kin.interview.InterviewAnswer.of("q-sector", "a")));

        assertTrue(blueprint.isComplete(state));
    }

    @Test
    void isComplete_conObligatoriaPendiente_deberiaSerFalse() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1)));

        assertFalse(blueprint.isComplete(emptyState()));
    }

    @Test
    void isComplete_presupuestoAgotado_deberiaSerTrue() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1)));

        var state = emptyState().withExchangeUsed(20);

        assertTrue(blueprint.isComplete(state));
    }

    @Test
    void isComplete_presupuestoIlimitado_noDebeAgotarse() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1)));

        assertFalse(blueprint.isComplete(InterviewState.empty(PROJECT_ID, 0).withExchangeUsed(100)));
    }

    @Test
    void next_siCompleta_deberiaEstarVacio() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1)));

        var state = emptyState()
            .withAnswered(java.util.Map.of("q-sector",
                com.kinplatform.kin.interview.InterviewAnswer.of("q-sector", "a")));

        assertTrue(blueprint.next(state).isEmpty());
    }

    @Test
    void pendingIds_deberiaExponerLasPendientesEnOrden() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo", true, 2),
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1),
            q("q-city", AnalyzedDimension.CITY, "ubicación", false, 3)));

        assertEquals(List.of("q-sector", "q-revenue", "q-city"), blueprint.pendingIds(emptyState()));

        var state = emptyState()
            .withAnswered(java.util.Map.of("q-sector",
                com.kinplatform.kin.interview.InterviewAnswer.of("q-sector", "a")));

        assertEquals(List.of("q-revenue", "q-city"), blueprint.pendingIds(state));
    }

    @Test
    void followUp_deberiaExigirPadreRespondido() {
        var parent = q("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", true, 1,
            List.of("q-det"));
        var det = q("q-det", AnalyzedDimension.REVENUE_MODEL, "detalle del modelo", false, 2);
        var extra = q("q-extra", AnalyzedDimension.SECTOR, "pendiente", true, 3);
        var blueprint = new InterviewBlueprint(List.of(parent, det, extra));

        assertEquals("q-revenue", blueprint.next(emptyState()).get().id());

        var answered = emptyState()
            .withAnswered(java.util.Map.of("q-revenue",
                com.kinplatform.kin.interview.InterviewAnswer.of("q-revenue", "suscripción")));

        assertEquals("q-det", blueprint.next(answered).get().id());
    }

    @Test
    void followUpObligatorio_deberiaNuncaEstarBloqueado() {
        var parent = q("q-p", AnalyzedDimension.SECTOR, "padre", false, 2);
        var det = q("q-det", AnalyzedDimension.SECTOR, "detalle", true, 1);
        var blueprint = new InterviewBlueprint(List.of(parent, det));

        assertEquals("q-det", blueprint.next(emptyState()).get().id());
    }

    @Test
    void blueprintVacio_deberiaEstarCompleto() {
        var blueprint = new InterviewBlueprint(List.of());

        assertEquals(0, blueprint.totalQuestions());
        assertTrue(blueprint.isComplete(emptyState()));
        assertTrue(blueprint.next(emptyState()).isEmpty());
        assertTrue(blueprint.pendingIds(emptyState()).isEmpty());
    }

    @Test
    void blueprintNulo_deberiaComportarseComoVacio() {
        var blueprint = new InterviewBlueprint(null);

        assertEquals(0, blueprint.totalQuestions());
        assertTrue(blueprint.isComplete(emptyState()));
    }

    @Test
    void identificadoresDuplicados_deberianLanzar() {
        assertThrows(IllegalArgumentException.class, () -> new InterviewBlueprint(List.of(
            q("q-a", AnalyzedDimension.SECTOR, "a", true, 1),
            q("q-a", AnalyzedDimension.CITY, "b", true, 2))));
    }

    @Test
    void preguntasNulas_deberianIgnorarse() {
        var withNull = new java.util.ArrayList<>(List.of(
            q("q-a", AnalyzedDimension.SECTOR, "a", true, 1)));
        withNull.add(null);
        var blueprint = new InterviewBlueprint(withNull);

        assertEquals(1, blueprint.totalQuestions());
        assertEquals("q-a", blueprint.next(emptyState()).get().id());
    }

    @Test
    void questions_deberiaSerInmutable() {
        var source = new ArrayList<>(List.of(q("q-a", AnalyzedDimension.SECTOR, "a", true, 1)));
        var blueprint = new InterviewBlueprint(source);

        source.clear();
        assertEquals(1, blueprint.totalQuestions());
        assertThrows(UnsupportedOperationException.class, () -> blueprint.questions().add(
            q("q-x", AnalyzedDimension.CITY, "x", true, 2)));
    }

    @Test
    void question_deberiaBuscarPorIdentificador() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1)));

        assertTrue(blueprint.question("q-sector").isPresent());
        assertTrue(blueprint.question("no-existe").isEmpty());
    }

    @Test
    void stateNulo_deberiaManejarseDefensivamente() {
        var blueprint = new InterviewBlueprint(List.of(
            q("q-sector", AnalyzedDimension.SECTOR, "sector", true, 1)));

        assertFalse(blueprint.isComplete(null));
        assertTrue(blueprint.next(null).isEmpty());
        assertTrue(blueprint.pendingIds(null).isEmpty());
    }
}
