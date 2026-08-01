package com.kinplatform.kin.ai;

import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromptAssemblerTest {

    @Test
    void promptRequest_deberiaExigirContextoParaConversacion() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");

        var exception = assertThrows(IllegalArgumentException.class,
            () -> PromptRequest.forConversation(null, decision));

        assertEquals("context es obligatorio para CONVERSATION", exception.getMessage());
    }

    @Test
    void promptRequest_deberiaExigirConversationDecisionParaConversacion() {
        var ctx = ProjectContext.fromProject("Mi App", null, "Software");

        var exception = assertThrows(IllegalArgumentException.class,
            () -> PromptRequest.forConversation(ctx, null));

        assertEquals("decision es obligatorio para CONVERSATION", exception.getMessage());
    }

    @Test
    void promptRequest_deberiaRechazarTipoNull() {
        var exception = assertThrows(IllegalArgumentException.class,
            () -> new PromptRequest(null, null, null, null));

        assertEquals("type no puede ser null", exception.getMessage());
    }

    @Test
    void promptRequest_deberiaExigirConsultingReportParaReporte() {
        var exception = assertThrows(IllegalArgumentException.class,
            () -> new PromptRequest(null, PromptType.REPORT, null, null));

        assertEquals("consultingReport es obligatorio para REPORT", exception.getMessage());
    }

    @Test
    void promptRequest_deberiaRechazarContextoODecisionParaReporte() {
        var ctx = ProjectContext.fromProject("Mi App", null, "Software");
        var report = ConsultingReport.empty();

        var exception = assertThrows(IllegalArgumentException.class,
            () -> new PromptRequest(report, PromptType.REPORT, ctx, null));

        assertEquals("context y decision deben ser null para REPORT", exception.getMessage());
    }

    @Test
    void assemble_deberiaDelegarPromptConversacionalAlConversationPromptBuilder() {
        var conversationBuilder = mock(ConversationPromptBuilder.class);
        var reportBuilder = mock(ReportPromptBuilder.class);
        var assembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.MVP, 7, "explorar plan de validación");
        var request = PromptRequest.forConversation(ctx, decision);
        when(conversationBuilder.build(request)).thenReturn("prompt conversacional");

        var prompt = assembler.assemble(request);

        assertEquals("prompt conversacional", prompt);
        verify(conversationBuilder).build(request);
        verify(reportBuilder, never()).build(any());
        verifyNoMoreInteractions(conversationBuilder, reportBuilder);
    }

    @Test
    void assemble_deberiaDelegarPromptDeReporteAlReportPromptBuilder() {
        var conversationBuilder = mock(ConversationPromptBuilder.class);
        var reportBuilder = mock(ReportPromptBuilder.class);
        var assembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var request = PromptRequest.forReport(ConsultingReport.empty());
        when(reportBuilder.build(request)).thenReturn("prompt de reporte");

        var prompt = assembler.assemble(request);

        assertEquals("prompt de reporte", prompt);
        verify(reportBuilder).build(request);
        verify(conversationBuilder, never()).build(any());
        verifyNoMoreInteractions(conversationBuilder, reportBuilder);
    }

    @Test
    void assemble_noDebeAgregarLogicaDeNegocioAlResultadoDelBuilder() {
        var conversationBuilder = mock(ConversationPromptBuilder.class);
        var reportBuilder = mock(ReportPromptBuilder.class);
        var assembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");
        var request = PromptRequest.forConversation(ctx, decision);
        when(conversationBuilder.build(request)).thenReturn("sentinel sin transformaciones");

        var prompt = assembler.assemble(request);

        assertEquals("sentinel sin transformaciones", prompt);
        assertFalse(prompt.contains("Mi App"));
        assertFalse(prompt.contains("explorar problema"));
        verify(conversationBuilder).build(request);
        verifyNoInteractions(reportBuilder);
    }
}
