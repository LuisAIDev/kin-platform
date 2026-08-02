package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.RiskDetectedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventStageOrderingTest {

    private final EventStage stage = new EventStage();

    @Test
    void orden_flujoReporte_deberiaSerAccionLuegoAnalisisLuegoCompletado() {
        var ctx = TestEventContexts.withRisks(
            TestEventContexts.withScore(
                TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 70),
            TestEventContexts.risk("R1", RiskLevel.HIGH));

        var events = stage.execute(ctx).events();
        int report = indexOf(events, ReportGeneratedEvent.class);
        int score = indexOf(events, ScoreCalculatedEvent.class);
        int risk = indexOf(events, RiskDetectedEvent.class);
        int completed = indexOf(events, ConversationCompletedEvent.class);

        assertTrue(report >= 0 && score >= 0 && risk >= 0 && completed >= 0);
        assertTrue(report < score);
        assertTrue(score < risk);
        assertTrue(risk < completed);
    }

    @Test
    void orden_flujoAsk_deberiaSerQuestionLuegoCompletado() {
        var ctx = TestEventContexts.ask(TestEventContexts.context());

        var events = stage.execute(ctx).events();
        int question = indexOf(events, QuestionGeneratedEvent.class);
        int completed = indexOf(events, ConversationCompletedEvent.class);

        assertTrue(question >= 0 && completed >= 0);
        assertTrue(question < completed);
    }

    @Test
    void sinDuplicados_flujoReporte_deberiaEmitirCadaTipoUnaVez() {
        var ctx = TestEventContexts.withRisks(
            TestEventContexts.withScore(
                TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 70),
            TestEventContexts.risk("R1", RiskLevel.HIGH));

        var events = stage.execute(ctx).events();

        assertEquals(1, count(events, ReportGeneratedEvent.class));
        assertEquals(1, count(events, ScoreCalculatedEvent.class));
        assertEquals(1, count(events, ConversationCompletedEvent.class));
        assertEquals(1, count(events, RiskDetectedEvent.class));
    }

    @Test
    void sinDuplicados_flujoAsk_deberiaEmitirCadaTipoUnaVez() {
        var ctx = TestEventContexts.ask(TestEventContexts.context());

        var events = stage.execute(ctx).events();

        assertEquals(1, count(events, QuestionGeneratedEvent.class));
        assertEquals(1, count(events, ConversationCompletedEvent.class));
    }

    @Test
    void orden_conversationCompleted_deberiaSerSiempreElUltimo() {
        var ctx = TestEventContexts.withRisks(
            TestEventContexts.withScore(
                TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 70),
            TestEventContexts.risk("R1", RiskLevel.HIGH));

        var events = stage.execute(ctx).events();

        assertTrue(events.get(events.size() - 1) instanceof ConversationCompletedEvent);
    }

    private static int indexOf(List<?> events, Class<?> type) {
        for (int i = 0; i < events.size(); i++) {
            if (type.isInstance(events.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static long count(List<?> events, Class<?> type) {
        return events.stream().filter(type::isInstance).count();
    }
}
