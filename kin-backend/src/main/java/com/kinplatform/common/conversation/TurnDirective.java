package com.kinplatform.common.conversation;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.decision.ConversationDecision;

/**
 * Política del turno: fase, acción, dimensión objetivo, modo de comunicación y
 * restricciones. La directiva enmarca la comunicación; la acción puntual de
 * transición la selecciona {@code ConversationStrategist} en el pipeline
 * (ADR-013).
 */
public record TurnDirective(
    ConversationPhase phase,
    ConversationDecision.Action action,
    AnalyzedDimension dimension,
    CommunicationMode communicationMode,
    TurnConstraints constraints
) {

    public TurnDirective {
        if (phase == null) {
            throw new IllegalArgumentException("phase no puede ser null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action no puede ser null");
        }
        if (communicationMode == null) {
            throw new IllegalArgumentException("communicationMode no puede ser null");
        }
        if (constraints == null) {
            throw new IllegalArgumentException("constraints no puede ser null");
        }
    }

    public boolean isQuestionTurn() {
        return communicationMode == CommunicationMode.QUESTION;
    }

    public boolean isReportTurn() {
        return communicationMode == CommunicationMode.EXPLAIN_REPORT;
    }
}


