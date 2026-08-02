package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventStageCompatibilityTest {

    private final EventStage stage = new EventStage();

    @Test
    void compatibilidad_deberiaConservarLosEventosExistentes() {
        var ctx = TestEventContexts.withScore(
            TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 65);

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().anyMatch(e -> e instanceof ReportGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ScoreCalculatedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
    }

    @Test
    void compatibilidad_type_deberiaSerConsistente() {
        var ctx = TestEventContexts.withScore(
            TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 65);

        var events = stage.execute(ctx).events();

        assertTrue(events.stream().anyMatch(e -> "report_generated".equals(((DomainEvent) e).type())));
        assertTrue(events.stream().anyMatch(e -> "score_calculated".equals(((DomainEvent) e).type())));
        assertTrue(events.stream().anyMatch(e -> "conversation_completed".equals(((DomainEvent) e).type())));
    }

    @Test
    void compatibilidad_soporteSinDecision_deberiaEmitirSoloCompletado() {
        var ctx = TestEventContexts.context();

        var result = stage.execute(ctx);

        assertEquals(1, result.events().size());
        assertTrue(result.events().get(0) instanceof ConversationCompletedEvent);
        assertEquals("UNKNOWN", ((ConversationCompletedEvent) result.events().get(0)).finalDecision());
    }

    @Test
    void compatibilidad_accionNoSoportada_deberiaEmitirSoloCompletado() {
        var ctx = TestEventContexts.context();
        ctx.decision(ConversationDecision.stop("fin"));

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().noneMatch(e -> e instanceof QuestionGeneratedEvent));
        assertTrue(result.events().stream().noneMatch(e -> e instanceof ReportGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
    }

    @Test
    void compatibilidad_aggregateId_deberiaSerElProjectId() {
        var ctx = TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context()));
        var expectedId = ctx.projectId();

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().allMatch(e -> expectedId.equals(((DomainEvent) e).aggregateId())));
    }

    @Test
    void compatibilidad_soporte_deberiaPermitirTurnosSinProjectContext() {
        var ctx = new PipelineContext(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
            "m", List.of(), "t", "d", "c");

        boolean supported = stage.supports(ctx);

        org.junit.jupiter.api.Assertions.assertFalse(supported);
    }
}
