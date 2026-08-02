package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.RiskDetectedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.risk.RiskResult;

/**
 * Etapa final del pipeline que publica los eventos de dominio según el flujo
 * real de la conversación (ADR-017, Etapa E4).
 *
 * <p>Semántica completa y aditiva:
 * <ol>
 *   <li>Evento de acción: {@code question_generated} (ASK) o
 *       {@code report_generated} (REPORT, solo si el {@code ConsultingReport}
 *       fue realmente generado).</li>
 *   <li>Eventos derivados del análisis (flujo REPORT): {@code score_calculated}
 *       cuando hay score y {@code risk_detected} por cada riesgo identificado.</li>
 *   <li>{@code conversation_completed} siempre, como cierre del turno
 *       (compatibilidad con el comportamiento existente).</li>
 * </ol>
 * Orden determinista y sin duplicados. No modifica consumidores existentes y
 * mantiene intactos los eventos ya publicados.</p>
 */
public class EventStage implements PipelineStage {

    @Override
    public String name() {
        return "Eventos";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.projectContext() != null;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var decision = context.decision();
        if (decision != null) {
            switch (decision.action()) {
                case ASK -> context.addEvent(new QuestionGeneratedEvent(
                    context.projectId(),
                    decision.dimension() != null ? decision.dimension().displayName() : "",
                    decision.explanation()
                ));
                case REPORT -> {
                    if (context.consultingReport() != null) {
                        context.addEvent(new ReportGeneratedEvent(
                            context.projectId(), "markdown"
                        ));
                    }
                }
                default -> {}
            }
        }
        if (context.scoreResult() != null && context.scoreResult().totalScore() > 0) {
            context.addEvent(new ScoreCalculatedEvent(
                context.projectId(),
                context.scoreResult().totalScore(),
                context.scoreResult().viabilityLabel(),
                context.projectContext().knownDimensionsCount()
            ));
        }
        emitRiskEvents(context);
        context.addEvent(new ConversationCompletedEvent(
            context.projectId(),
            context.projectContext().exchangeCount(),
            context.projectContext().knownDimensionsCount(),
            decision != null ? decision.action().name() : "UNKNOWN"
        ));
        return context;
    }

    /**
     * Emite un {@link RiskDetectedEvent} por cada riesgo del resultado del
     * {@code RiskEngine} cuando el flujo de reporte los produjo (ADR-017, E4).
     */
    private void emitRiskEvents(PipelineContext context) {
        RiskResult riskResult = context.riskResult();
        if (riskResult == null || riskResult.isEmpty()) {
            return;
        }
        for (var risk : riskResult.risks()) {
            context.addEvent(new RiskDetectedEvent(
                context.projectId(),
                risk.title(),
                risk.severity().name()
            ));
        }
    }
}
