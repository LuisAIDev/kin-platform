package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationPromptBuilderDirectiveTest {

    private final ConversationPromptBuilder builder = new ConversationPromptBuilder();

    private final ProjectContext context =
            ProjectContext.fromProject("Mi App", "App de gestión", "Software");
    private final ConversationDecision decision =
            ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");

    @Test
    void build_conDirectiva_deberiaEnmarcarFaseModoYRestricciones() {
        var directive = new TurnDirective(
            ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK,
            AnalyzedDimension.PROBLEM, CommunicationMode.QUESTION, TurnConstraints.question());

        var prompt = builder.build(PromptRequest.forConversation(context, decision, directive));

        assertTrue(prompt.contains("## DIRECTIVA DE COMUNICACIÓN"));
        assertTrue(prompt.contains(ConversationPhase.EXPLORATION.name()));
        assertTrue(prompt.contains(CommunicationMode.QUESTION.name()));
        assertTrue(prompt.contains(String.valueOf(TurnConstraints.QUESTION_MAX_LENGTH)));
        assertTrue(prompt.contains("Una sola pregunta por turno: sí"));
        assertTrue(prompt.contains("=== CONSULTING REPORT ==="));
        assertTrue(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
    }

    @Test
    void build_conDirectivaDeReporte_deberiaEnmarcarLasRestriccionesDeReporte() {
        var directive = new TurnDirective(
            ConversationPhase.REPORTING, ConversationDecision.Action.REPORT,
            null, CommunicationMode.EXPLAIN_REPORT, TurnConstraints.reportExplanation());

        var prompt = builder.build(PromptRequest.forConversation(context, decision, directive));

        assertTrue(prompt.contains("## DIRECTIVA DE COMUNICACIÓN"));
        assertTrue(prompt.contains(ConversationPhase.REPORTING.name()));
        assertTrue(prompt.contains(CommunicationMode.EXPLAIN_REPORT.name()));
        assertTrue(prompt.contains(String.valueOf(TurnConstraints.REPORT_EXPLANATION_MAX_LENGTH)));
        assertTrue(prompt.contains("Una sola pregunta por turno: no"));
    }

    @Test
    void build_sinDirectiva_deberiaConservarElPromptOriginalSinSeccionDeDirectiva() {
        var prompt = builder.build(PromptRequest.forConversation(context, decision));

        assertFalse(prompt.contains("## DIRECTIVA DE COMUNICACIÓN"));
        assertTrue(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
        assertTrue(prompt.contains("explorar problema"));
    }
}
