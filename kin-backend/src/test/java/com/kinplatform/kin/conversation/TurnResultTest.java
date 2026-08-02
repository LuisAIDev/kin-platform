package com.kinplatform.kin.conversation;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnResultTest {

    private final ProjectContext context = ProjectContext.fromProject("Proyecto", "Desc", "TECH");
    private final ConversationDecision decision =
        ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta");
    private final TurnDirective directive = new TurnDirective(
        ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK,
        AnalyzedDimension.TARGET_CUSTOMER, CommunicationMode.QUESTION, TurnConstraints.question());

    @Test
    void result_deberiaExponerCampos() {
        var event = new QuestionGeneratedEvent(UUID.randomUUID(), "TARGET_CUSTOMER", "razón");
        var result = new TurnResult(context, decision, directive, "¿Quiénes son tus clientes?",
            ResponseValidation.ok(), null, List.of(event));

        assertSame(context, result.projectContext());
        assertSame(decision, result.decision());
        assertSame(directive, result.directive());
        assertEquals("¿Quiénes son tus clientes?", result.aiResponse());
        assertTrue(result.validation().accepted());
        assertNull(result.consultingReport());
        assertEquals(List.of(event), result.events());
    }

    @Test
    void result_deberiaSoportarReporte() {
        var reporte = ConsultingReport.empty();
        var result = new TurnResult(context,
            ConversationDecision.generateReport("reporte"), directive, "explicación",
            ResponseValidation.ok(), reporte, List.of());

        assertSame(reporte, result.consultingReport());
    }

    @Test
    void result_deberiaProtegerListaDeEventos() {
        var events = new ArrayList<DomainEvent>(
            List.of(new QuestionGeneratedEvent(UUID.randomUUID(), "d", "r")));
        var result = new TurnResult(context, decision, directive, "resp",
            ResponseValidation.ok(), null, events);

        events.clear();
        assertThrows(UnsupportedOperationException.class,
            () -> result.events().clear());
        assertEquals(1, result.events().size());
    }

    @Test
    void result_deberiaAceptarEventosNulos() {
        var result = new TurnResult(context, decision, directive, "resp",
            ResponseValidation.ok(), null, null);
        assertTrue(result.events().isEmpty());
    }

    @Test
    void result_deberiaRechazarCamposObligatoriosNulos() {
        assertThrows(IllegalArgumentException.class, () -> new TurnResult(
            null, decision, directive, "resp", ResponseValidation.ok(), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TurnResult(
            context, null, directive, "resp", ResponseValidation.ok(), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TurnResult(
            context, decision, null, "resp", ResponseValidation.ok(), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TurnResult(
            context, decision, directive, "resp", null, null, List.of()));
    }
}
