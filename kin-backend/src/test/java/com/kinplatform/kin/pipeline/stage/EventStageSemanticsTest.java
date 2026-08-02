package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.RiskDetectedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventStageSemanticsTest {

    private final EventStage stage = new EventStage();

    @Test
    void ask_deberiaEmitirQuestionGeneratedYCompletado() {
        var ctx = TestEventContexts.ask(TestEventContexts.context());

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().anyMatch(e -> e instanceof QuestionGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
        assertTrue(result.events().stream().noneMatch(e -> e instanceof ReportGeneratedEvent));
        assertTrue(result.events().stream().noneMatch(e -> e instanceof ScoreCalculatedEvent));
        assertTrue(result.events().stream().noneMatch(e -> e instanceof RiskDetectedEvent));
    }

    @Test
    void report_conReporte_deberiaEmitirReportGenerated() {
        var ctx = TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context()));

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().anyMatch(e -> e instanceof ReportGeneratedEvent));
    }

    @Test
    void report_sinReporte_deberiaOmitirReportGenerated() {
        var ctx = TestEventContexts.report(TestEventContexts.context());

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().noneMatch(e -> e instanceof ReportGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
    }

    @Test
    void report_deberiaEmitirScoreYEventosDeRiesgo() {
        var ctx = TestEventContexts.withRisks(
            TestEventContexts.withScore(
                TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())),
                75),
            TestEventContexts.risk("R1", RiskLevel.HIGH));

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().anyMatch(e -> e instanceof ScoreCalculatedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof RiskDetectedEvent));
        assertTrue(result.events().stream().noneMatch(e -> e instanceof QuestionGeneratedEvent));
    }

    @Test
    void flujoConDosRiesgos_deberiaEmitirUnRiskDetectedPorRiesgo() {
        var ctx = TestEventContexts.withRisks(
            TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())),
            TestEventContexts.risk("R1", RiskLevel.HIGH),
            TestEventContexts.risk("R2", RiskLevel.CRITICAL));

        var result = stage.execute(ctx);

        long riskEvents = result.events().stream()
            .filter(e -> e instanceof RiskDetectedEvent)
            .count();
        assertEquals(2, riskEvents);
    }

    @Test
    void flujoSinRiesgos_deberiaOmitirRiskDetected() {
        var ctx = TestEventContexts.withScore(
            TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 60);

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().noneMatch(e -> e instanceof RiskDetectedEvent));
    }

    @Test
    void scoreCero_deberiaOmitirScoreCalculated() {
        var ctx = TestEventContexts.withScore(
            TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())), 0);

        var result = stage.execute(ctx);

        assertTrue(result.events().stream().noneMatch(e -> e instanceof ScoreCalculatedEvent));
    }

    @Test
    void conversationCompleted_deberiaEmitirseSiempre() {
        var ask = stage.execute(TestEventContexts.ask(TestEventContexts.context()));
        var report = stage.execute(TestEventContexts.report(TestEventContexts.context()));

        assertTrue(ask.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
        assertTrue(report.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
    }

    @Test
    void riskDetected_deberiaTransportarTituloYSeveridad() {
        var ctx = TestEventContexts.withRisks(
            TestEventContexts.withReport(TestEventContexts.report(TestEventContexts.context())),
            TestEventContexts.risk("Riesgo de mercado", RiskLevel.HIGH));

        var result = stage.execute(ctx);

        RiskDetectedEvent event = result.events().stream()
            .filter(e -> e instanceof RiskDetectedEvent)
            .map(e -> (RiskDetectedEvent) e)
            .findFirst()
            .orElseThrow();
        assertEquals("Riesgo de mercado", event.risk());
        assertEquals("HIGH", event.severity());
    }
}
