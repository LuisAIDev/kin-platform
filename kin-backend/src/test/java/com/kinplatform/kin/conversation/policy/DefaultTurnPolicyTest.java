package com.kinplatform.kin.conversation.policy;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTurnPolicyTest {

    private final DefaultTurnPolicy policy = new DefaultTurnPolicy();
    private final ProjectContext context = ProjectContext.fromProject("Proyecto", "Desc", "TECH");

    private ProjectContext contextoConReporte() {
        var ctx = ProjectContext.fromProject("Proyecto", "Desc", "TECH");
        ctx.markReportGenerated();
        return ctx;
    }

    private ConversationDecision decision(ConversationDecision.Action action) {
        return new ConversationDecision(action, null, 5, "razón", Map.of());
    }

    @Test
    void ask_deberiaSerExplorationQuestion() {
        var decision = ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta");

        var directive = policy.decide(context, decision);

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertSame(CommunicationMode.QUESTION, directive.communicationMode());
        assertSame(ConversationDecision.Action.ASK, directive.action());
        assertSame(AnalyzedDimension.TARGET_CUSTOMER, directive.dimension());
        assertEquals(TurnConstraints.question(), directive.constraints());
    }

    @Test
    void report_deberiaSerReportingExplainReport() {
        var directive = policy.decide(context, ConversationDecision.generateReport("reporte"));

        assertSame(ConversationPhase.REPORTING, directive.phase());
        assertSame(CommunicationMode.EXPLAIN_REPORT, directive.communicationMode());
        assertSame(ConversationDecision.Action.REPORT, directive.action());
        assertNull(directive.dimension());
        assertEquals(TurnConstraints.reportExplanation(), directive.constraints());
    }

    @Test
    void stop_deberiaSerClosedFarewell() {
        var directive = policy.decide(context, ConversationDecision.stop("fin"));

        assertSame(ConversationPhase.CLOSED, directive.phase());
        assertSame(CommunicationMode.FAREWELL, directive.communicationMode());
        assertSame(ConversationDecision.Action.STOP, directive.action());
        assertEquals(TurnConstraints.question(), directive.constraints());
    }

    @Test
    void escalate_deberiaSerClosedFarewell() {
        var directive = policy.decide(context, decision(ConversationDecision.Action.ESCALATE));

        assertSame(ConversationPhase.CLOSED, directive.phase());
        assertSame(CommunicationMode.FAREWELL, directive.communicationMode());
        assertSame(ConversationDecision.Action.ESCALATE, directive.action());
    }

    @Test
    void summarize_sinReporte_deberiaSerExplorationSummary() {
        var directive = policy.decide(context, decision(ConversationDecision.Action.SUMMARIZE));

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
        assertEquals(TurnConstraints.reportExplanation(), directive.constraints());
    }

    @Test
    void summarize_conReporteGenerado_deberiaSerReportingSummary() {
        var directive = policy.decide(contextoConReporte(), decision(ConversationDecision.Action.SUMMARIZE));

        assertSame(ConversationPhase.REPORTING, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
    }

    @Test
    void summarize_conConsultingReport_deberiaSerReportingSummary() {
        var directive = policy.decide(context,
            decision(ConversationDecision.Action.SUMMARIZE), ConsultingReport.empty());

        assertSame(ConversationPhase.REPORTING, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
    }

    @Test
    void recommend_deberiaSerSummaryExploration() {
        var directive = policy.decide(context, decision(ConversationDecision.Action.RECOMMEND));

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
        assertEquals(TurnConstraints.reportExplanation(), directive.constraints());
    }

    @Test
    void validate_deberiaSerSummaryExploration() {
        var directive = policy.decide(context, decision(ConversationDecision.Action.VALIDATE));

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
    }

    @Test
    void recommend_conReporte_deberiaSerReporting() {
        var directive = policy.decide(contextoConReporte(), decision(ConversationDecision.Action.RECOMMEND));

        assertSame(ConversationPhase.REPORTING, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
    }

    @Test
    void reporteNulo_conContextoSinReporte_deberiaSerExploration() {
        var directive = policy.decide(context,
            decision(ConversationDecision.Action.RECOMMEND), null);

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
    }

    @Test
    void transicion_deExplorationAReporting() {
        var exploration = policy.decide(context,
            ConversationDecision.ask(AnalyzedDimension.RISKS, 6, "pregunta"));
        assertSame(ConversationPhase.EXPLORATION, exploration.phase());

        var reporting = policy.decide(contextoConReporte(), ConversationDecision.generateReport("reporte"));
        assertSame(ConversationPhase.REPORTING, reporting.phase());
    }

    @Test
    void transicion_deReportingAClosed() {
        var reporting = policy.decide(contextoConReporte(), ConversationDecision.generateReport("reporte"));
        assertSame(ConversationPhase.REPORTING, reporting.phase());

        var closed = policy.decide(contextoConReporte(), ConversationDecision.stop("fin"));
        assertSame(ConversationPhase.CLOSED, closed.phase());
    }

    @Test
    void contextoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> policy.decide(null, ConversationDecision.ask(AnalyzedDimension.RISKS, 6, "pregunta")));
    }

    @Test
    void decisionNula_deberiaTratarseComoInicioDeExploracion() {
        var directive = policy.decide(context, null);

        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertSame(ConversationDecision.Action.ASK, directive.action());
        assertSame(CommunicationMode.QUESTION, directive.communicationMode());
        assertEquals(TurnConstraints.question(), directive.constraints());
    }

    @Test
    void consultaDeDosArgumentos_deberiaDelegarConReporteNulo() {
        var decision = ConversationDecision.ask(AnalyzedDimension.RISKS, 6, "pregunta");
        assertEquals(policy.decide(context, decision), policy.decide(context, decision, null));
    }

    @Test
    void determinismo_deberiaProducirMismaDirectiva() {
        var decision = ConversationDecision.ask(AnalyzedDimension.RISKS, 6, "pregunta");
        var first = policy.decide(context, decision);
        var second = policy.decide(context, decision);
        assertEquals(first, second);
    }

    @Test
    void restricciones_question_deberianSerCorrectas() {
        var constraints = TurnConstraints.question();
        assertEquals(280, constraints.maxLength());
        assertTrue(constraints.singleQuestion());
        assertTrue(constraints.forbiddenMarkers().contains("=== CONSULTING REPORT ==="));
        assertTrue(constraints.forbiddenMarkers().contains("## INFORME DE VIABILIDAD"));
        assertTrue(constraints.forbiddenMarkers().contains("Scoring:"));
    }

    @Test
    void restricciones_reportExplanation_deberianSerCorrectas() {
        var constraints = TurnConstraints.reportExplanation();
        assertEquals(1200, constraints.maxLength());
        assertTrue(!constraints.singleQuestion());
        assertTrue(constraints.forbiddenMarkers().isEmpty());
    }

    @Test
    void directiva_deberiaSerInmutable() {
        var directive = policy.decide(context,
            ConversationDecision.ask(AnalyzedDimension.RISKS, 6, "pregunta"));
        assertThrows(UnsupportedOperationException.class,
            () -> directive.constraints().forbiddenMarkers().add("otro"));
    }

    @Test
    void interfazTurnPolicy_deberiaFuncionarConDefault() {
        TurnPolicy asInterface = new DefaultTurnPolicy();
        var directive = asInterface.decide(context,
            ConversationDecision.ask(AnalyzedDimension.RISKS, 6, "pregunta"));
        assertSame(ConversationPhase.EXPLORATION, directive.phase());
        assertEquals(TurnConstraints.question(), directive.constraints());
    }

    @Test
    void directiva_deberiaExponerCamposResueltos() {
        var directive = policy.decide(contextoConReporte(),
            decision(ConversationDecision.Action.VALIDATE));
        assertSame(ConversationPhase.REPORTING, directive.phase());
        assertSame(CommunicationMode.SUMMARY, directive.communicationMode());
        assertSame(ConversationDecision.Action.VALIDATE, directive.action());
        assertNull(directive.dimension());
    }
}
