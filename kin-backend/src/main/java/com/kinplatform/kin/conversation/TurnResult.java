package com.kinplatform.kin.conversation;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;

import java.util.List;

/**
 * Output tipado de un turno de conversación. Agrega el estado persistido del
 * proyecto, la decisión, la directiva, la respuesta del LLM, la validación de
 * la comunicación y (en fase REPORTING) el {@code ConsultingReport}.
 */
public record TurnResult(
    ProjectContext projectContext,
    ConversationDecision decision,
    TurnDirective directive,
    String aiResponse,
    ResponseValidation validation,
    ConsultingReport consultingReport,
    List<DomainEvent> events
) {

    public TurnResult {
        if (projectContext == null) {
            throw new IllegalArgumentException("projectContext no puede ser null");
        }
        if (decision == null) {
            throw new IllegalArgumentException("decision no puede ser null");
        }
        if (directive == null) {
            throw new IllegalArgumentException("directive no puede ser null");
        }
        if (validation == null) {
            throw new IllegalArgumentException("validation no puede ser null");
        }
        events = (events != null) ? List.copyOf(events) : List.of();
    }
}
