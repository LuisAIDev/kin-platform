package com.kinplatform.kin.conversation.policy;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;

/**
 * Política de turno determinista del Conversation Orchestrator (ADR-013).
 *
 * <p>Resuelve en Java la fase, el modo de comunicación y las restricciones de
 * cada turno a partir de la decisión previa ({@code ConversationDecision}), del
 * contexto persistido del proyecto y, opcionalmente, del
 * {@code ConsultingReport} generado. La acción puntual de transición la
 * selecciona {@code ConversationStrategist} en el pipeline; esta política
 * únicamente enmarca la comunicación que el LLM puede producir.</p>
 *
 * <p>El mapeo es exhaustivo y determinista: las 7 acciones de
 * {@code ConversationDecision.Action} se cubren con los 4 modos de
 * comunicación, y la fase se resuelve con la acción y la presencia de reporte
 * (proporcionado o ya generado).</p>
 */
public class DefaultTurnPolicy implements TurnPolicy {

    @Override
    public TurnDirective decide(ProjectContext context, ConversationDecision previousDecision) {
        return decide(context, previousDecision, null);
    }

    /**
     * Resuelve la directiva de turno considerando un reporte de consultoría
     * opcional (presente cuando la fase REPORTING ya se ha ejecutado).
     *
     * @param context          contexto persistido del proyecto (obligatorio)
     * @param previousDecision decisión previa de la conversación; {@code null}
     *                         en el primer turno, que se trata como inicio de
     *                         exploración (ASK)
     * @param consultingReport reporte generado (opcional)
     */
    public TurnDirective decide(ProjectContext context,
                                ConversationDecision previousDecision,
                                ConsultingReport consultingReport) {
        if (context == null) {
            throw new IllegalArgumentException("context no puede ser null");
        }

        ConversationDecision decision = previousDecision;
        ConversationDecision.Action action = decision != null
                ? decision.action()
                : ConversationDecision.Action.ASK;
        AnalyzedDimension dimension = decision != null ? decision.dimension() : null;

        boolean hasReport = consultingReport != null || context.reportGenerated();
        CommunicationMode mode = resolveMode(action);
        ConversationPhase phase = resolvePhase(action, hasReport);
        TurnConstraints constraints = resolveConstraints(mode);

        return new TurnDirective(phase, action, dimension, mode, constraints);
    }

    private CommunicationMode resolveMode(ConversationDecision.Action action) {
        return switch (action) {
            case ASK -> CommunicationMode.QUESTION;
            case REPORT -> CommunicationMode.EXPLAIN_REPORT;
            case RECOMMEND, VALIDATE, SUMMARIZE -> CommunicationMode.SUMMARY;
            case STOP, ESCALATE -> CommunicationMode.FAREWELL;
        };
    }

    private ConversationPhase resolvePhase(ConversationDecision.Action action, boolean hasReport) {
        return switch (action) {
            case STOP, ESCALATE -> ConversationPhase.CLOSED;
            case REPORT -> ConversationPhase.REPORTING;
            case ASK -> ConversationPhase.EXPLORATION;
            case RECOMMEND, VALIDATE, SUMMARIZE ->
                    hasReport ? ConversationPhase.REPORTING : ConversationPhase.EXPLORATION;
        };
    }

    private TurnConstraints resolveConstraints(CommunicationMode mode) {
        return switch (mode) {
            case QUESTION, FAREWELL -> TurnConstraints.question();
            case EXPLAIN_REPORT, SUMMARY -> TurnConstraints.reportExplanation();
        };
    }
}
