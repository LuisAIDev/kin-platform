package com.kinplatform.kin.ai;

import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.InterviewDecision;
import com.kinplatform.kin.interview.InterviewDirective;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromptAssemblerInterviewTest {

    private InterviewResult resultadoEntrevistaActiva() {
        var directive = InterviewDirective.of("q-sector", AnalyzedDimension.SECTOR,
            "sector y giro del negocio", AnswerRules.defaults());
        var state = InterviewState.empty(UUID.randomUUID())
            .withCurrent("q-sector").withPending(List.of("q-sector"));
        return InterviewResult.of(InterviewDecision.ask("q-sector", "Falta información"),
            directive, state, state.toProgress(5));
    }

    @Test
    void assemble_conEntrevista_deberiaDelegarConInterviewResultAlConversationBuilder() {
        var conversationBuilder = mock(ConversationPromptBuilder.class);
        var reportBuilder = mock(ReportPromptBuilder.class);
        var assembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.SECTOR, 9, "entrevista");
        var request = PromptRequest.forConversation(ctx, decision);
        var interview = resultadoEntrevistaActiva();
        when(conversationBuilder.build(request, interview)).thenReturn("prompt de entrevista");

        var prompt = assembler.assemble(request, interview);

        assertEquals("prompt de entrevista", prompt);
        verify(conversationBuilder).build(request, interview);
        verify(reportBuilder, never()).build(any());
    }

    @Test
    void assemble_sinEntrevista_deberiaDelegarSinInterviewResultAlConversationBuilder() {
        var conversationBuilder = mock(ConversationPromptBuilder.class);
        var reportBuilder = mock(ReportPromptBuilder.class);
        var assembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.SECTOR, 9, "entrevista");
        var request = PromptRequest.forConversation(ctx, decision);
        when(conversationBuilder.build(request, null)).thenReturn("prompt conversacional");

        var prompt = assembler.assemble(request, null);

        assertEquals("prompt conversacional", prompt);
        verify(conversationBuilder).build(request, null);
        verify(reportBuilder, never()).build(any());
    }

    @Test
    void assemble_modoReporte_deberiaIgnorarElResultadoDeEntrevista() {
        var conversationBuilder = mock(ConversationPromptBuilder.class);
        var reportBuilder = mock(ReportPromptBuilder.class);
        var assembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var request = PromptRequest.forReport(ConsultingReport.empty());
        when(reportBuilder.build(request)).thenReturn("prompt de reporte");

        var prompt = assembler.assemble(request, resultadoEntrevistaActiva());

        assertEquals("prompt de reporte", prompt);
        verify(reportBuilder).build(request);
        verify(conversationBuilder, never()).build(any(), any());
        verify(conversationBuilder, never()).build(any());
    }
}
