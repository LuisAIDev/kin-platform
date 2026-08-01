package com.kinplatform.kin.conversation;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.decision.ConversationDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnDirectiveTest {

    @Test
    void directive_deberiaExponerCampos() {
        var constraints = TurnConstraints.question();
        var directive = new TurnDirective(
            ConversationPhase.EXPLORATION,
            ConversationDecision.Action.ASK,
            AnalyzedDimension.TARGET_CUSTOMER,
            CommunicationMode.QUESTION,
            constraints);

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertSame(ConversationDecision.Action.ASK, directive.action());
        assertSame(AnalyzedDimension.TARGET_CUSTOMER, directive.dimension());
        assertSame(CommunicationMode.QUESTION, directive.communicationMode());
        assertSame(constraints, directive.constraints());
        assertTrue(directive.isQuestionTurn());
        assertFalse(directive.isReportTurn());
    }

    @Test
    void directive_deberiaReconocerTurnoDeReporte() {
        var directive = new TurnDirective(
            ConversationPhase.REPORTING,
            ConversationDecision.Action.REPORT,
            null,
            CommunicationMode.EXPLAIN_REPORT,
            TurnConstraints.reportExplanation());

        assertTrue(directive.isReportTurn());
        assertFalse(directive.isQuestionTurn());
        assertNull(directive.dimension());
    }

    @Test
    void directive_deberiaRechazarCamposNulos() {
        var constraints = TurnConstraints.question();
        assertThrows(IllegalArgumentException.class, () -> new TurnDirective(
            null, ConversationDecision.Action.ASK, null, CommunicationMode.QUESTION, constraints));
        assertThrows(IllegalArgumentException.class, () -> new TurnDirective(
            ConversationPhase.EXPLORATION, null, null, CommunicationMode.QUESTION, constraints));
        assertThrows(IllegalArgumentException.class, () -> new TurnDirective(
            ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK, null, null, constraints));
        assertThrows(IllegalArgumentException.class, () -> new TurnDirective(
            ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK, null,
            CommunicationMode.QUESTION, null));
    }

    @Test
    void directive_deberiaSoportarModosNoPreguntaNoReporte() {
        var directive = new TurnDirective(
            ConversationPhase.CLOSED,
            ConversationDecision.Action.STOP,
            null,
            CommunicationMode.FAREWELL,
            TurnConstraints.question());
        assertFalse(directive.isQuestionTurn());
        assertFalse(directive.isReportTurn());
    }

    @Test
    void directive_deberiaSoportarResumen() {
        var directive = new TurnDirective(
            ConversationPhase.REPORTING,
            ConversationDecision.Action.SUMMARIZE,
            null,
            CommunicationMode.SUMMARY,
            TurnConstraints.reportExplanation());
        assertFalse(directive.isQuestionTurn());
        assertFalse(directive.isReportTurn());
        assertEquals(CommunicationMode.SUMMARY, directive.communicationMode());
    }
}
