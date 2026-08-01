package com.kinplatform.kin.ai;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromptRequestDirectiveTest {

    private final ProjectContext context =
            ProjectContext.fromProject("Mi App", "App de gestión", "Software");
    private final ConversationDecision decision =
            ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");
    private final TurnDirective directive = new TurnDirective(
            ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK,
            AnalyzedDimension.PROBLEM, CommunicationMode.QUESTION, TurnConstraints.question());

    @Test
    void forConversation_sinDirectiva_deberiaConservarElFactoryDeADR012() {
        var request = PromptRequest.forConversation(context, decision);

        assertEquals(PromptType.CONVERSATION, request.type());
        assertSame(context, request.context());
        assertSame(decision, request.decision());
        assertNull(request.consultingReport());
        assertNull(request.directive());
    }

    @Test
    void forConversation_conDirectiva_deberiaPreservarLaDirectiva() {
        var request = PromptRequest.forConversation(context, decision, directive);

        assertEquals(PromptType.CONVERSATION, request.type());
        assertSame(context, request.context());
        assertSame(decision, request.decision());
        assertNull(request.consultingReport());
        assertSame(directive, request.directive());
    }

    @Test
    void constructorCuatroArgumentos_deberiaNormalizarLaDirectivaANull() {
        var request = new PromptRequest(null, PromptType.CONVERSATION, context, decision);

        assertEquals(PromptType.CONVERSATION, request.type());
        assertNull(request.directive());
    }

    @Test
    void forReport_deberiaConservarLaFronteraSinDirectiva() {
        var reporte = ConsultingReport.empty();

        var request = PromptRequest.forReport(reporte);

        assertEquals(PromptType.REPORT, request.type());
        assertSame(reporte, request.consultingReport());
        assertNull(request.context());
        assertNull(request.decision());
        assertNull(request.directive());
    }

    @Test
    void forReport_conDirectivaEnConstructor_deberiaLanzar() {
        var reporte = ConsultingReport.empty();

        assertThrows(IllegalArgumentException.class,
            () -> new PromptRequest(reporte, PromptType.REPORT, null, null, directive));
    }

    @Test
    void conversacion_sinDecision_deberiaSeguirLanzando() {
        assertThrows(IllegalArgumentException.class,
            () -> PromptRequest.forConversation(context, null, directive));
    }
}
