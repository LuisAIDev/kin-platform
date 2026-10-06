package com.kinplatform.kin.conversation;

import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * Resultado de un turno de conversación en modo streaming (SSE).
 *
 * <p>El {@link Flux} transporta los tokens de la respuesta del LLM y la
 * decisión/reporte del turno se entregan junto al flujo para que la capa de
 * I/O pueda persistirlos sin re-ejecutar el pipeline (aditivo, ADR-013). En un
 * turno {@code REPORT}, {@code consultingReport} porta el informe de
 * consultoría ya generado; en caso contrario es {@code null}.</p>
 *
 * <p>{@code events} entrega los {@link DomainEvent} producidos por el pipeline
 * para este turno: la capa de I/O los publica en el outbox transaccional
 * DENTRO de la transacción corta de finalización del turno (ADR-026). KinMethod
 * ya no los publica directamente.</p>
 */
public record StreamingTurnOutcome(
        Flux<String> flux, ConversationDecision decision, ConsultingReport consultingReport, List<DomainEvent> events) {

    public StreamingTurnOutcome {
        if (flux == null) {
            throw new IllegalArgumentException("flux no puede ser null");
        }
        events = (events != null) ? List.copyOf(events) : List.of();
    }

    /**
     * Constructor de compatibilidad sin eventos (los tests que mockean el
     * orquestador no necesitan transportar eventos en el outcome).
     */
    public StreamingTurnOutcome(Flux<String> flux, ConversationDecision decision, ConsultingReport consultingReport) {
        this(flux, decision, consultingReport, List.of());
    }
}

