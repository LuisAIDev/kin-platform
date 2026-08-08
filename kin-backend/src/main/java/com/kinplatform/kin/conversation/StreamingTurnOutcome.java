package com.kinplatform.kin.conversation;

import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import reactor.core.publisher.Flux;

/**
 * Resultado de un turno de conversación en modo streaming (SSE).
 *
 * <p>El {@link Flux} transporta los tokens de la respuesta del LLM y la
 * decisión/reporte del turno se entregan junto al flujo para que la capa de
 * I/O pueda persistirlos sin re-ejecutar el pipeline (aditivo, ADR-013). En un
 * turno {@code REPORT}, {@code consultingReport} porta el informe de
 * consultoría ya generado; en caso contrario es {@code null}.</p>
 */
public record StreamingTurnOutcome(
        Flux<String> flux,
        ConversationDecision decision,
        ConsultingReport consultingReport
) {

    public StreamingTurnOutcome {
        if (flux == null) {
            throw new IllegalArgumentException("flux no puede ser null");
        }
    }
}
