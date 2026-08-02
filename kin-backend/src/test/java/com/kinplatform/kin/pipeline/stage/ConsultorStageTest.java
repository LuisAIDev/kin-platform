package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;
import com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter;
import com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.InterviewDecision;
import com.kinplatform.kin.interview.InterviewDirective;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultorStageTest {

    @Mock
    private AIResponder aiResponder;

    private ConversationPromptBuilder conversationBuilder;
    private ReportPromptBuilder reportBuilder;
    private PromptAssembler promptAssembler;
    private ConsultorStage stage;

    @BeforeEach
    void setUp() {
        conversationBuilder = new ConversationPromptBuilder();
        reportBuilder = new ReportPromptBuilder(List.of(
            new ExecutiveSummaryFormatter(),
            new ScoresSectionFormatter(),
            new RecommendationsSectionFormatter(),
            new RisksSectionFormatter(),
            new OpportunitiesSectionFormatter(),
            new FinancialSectionFormatter(),
            new MarketSectionFormatter(),
            new InnovationSectionFormatter(),
            new NextStepsSectionFormatter(),
            new ReportMetadataFormatter()
        ));
        promptAssembler = new PromptAssembler(conversationBuilder, reportBuilder);
        stage = new ConsultorStage(aiResponder, promptAssembler);
    }

    private PipelineContext context(boolean streaming) {
        var ctx = new PipelineContext(
            UUID.randomUUID(), UUID.randomUUID(), "hola", List.of(),
            "Mi App", "App de gestión", "Software");
        var projectContext = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");
        projectContext.attachDecision(decision);
        ctx.projectContext(projectContext);
        ctx.decision(decision);
        ctx.streaming(streaming);
        return ctx;
    }

    @Test
    void name_deberiaSerConsultor() {
        assertEquals("Consultor", stage.name());
    }

    @Test
    void supports_deberiaSerSiempreVerdadero() {
        assertTrue(stage.supports(context(false)));
    }

    @Test
    void execute_bloqueante_deberiaEscribirLaRespuestaYNoUsarStreaming() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("respuesta de KIN");

        var ctx = context(false);
        var result = stage.execute(ctx);

        assertSame(ctx, result);
        assertEquals("respuesta de KIN", ctx.aiResponse());
        assertNull(ctx.aiResponseFlux());
        verify(aiResponder, never()).respondStream(any());
    }

    @Test
    void execute_streaming_deberiaGuardarElFluxSinBloquear() {
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just("a", "b"));

        var ctx = context(true);
        var result = stage.execute(ctx);

        assertSame(ctx, result);
        assertNull(ctx.aiResponse());
        assertNotNull(ctx.aiResponseFlux());
        verify(aiResponder, never()).respond(any());
    }

    @Test
    void execute_deberiaEnviarElPromptEnsambladoAlResponder() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var request = captor.getValue();
        assertEquals("hola", request.userMessage());
        assertTrue(request.systemPrompt().contains("Mi App"));
        assertTrue(request.systemPrompt().contains("App de gestión"));
        assertTrue(request.systemPrompt().contains("## INSTRUCCIÓN ESTRATÉGICA"));
        assertTrue(request.systemPrompt().contains("explorar problema"));
        assertTrue(request.history().isEmpty());
    }

    @Test
    void execute_modoReporte_deberiaUsarElPromptDeReporte() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        var decision = ConversationDecision.generateReport("contexto completo");
        ctx.decision(decision);
        ctx.consultingReport(com.kinplatform.kin.reporting.report.model.ConsultingReport.empty());
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var request = captor.getValue();
        assertTrue(request.systemPrompt().contains("=== CONSULTING REPORT ==="));
        assertTrue(request.systemPrompt().contains("--- INSTRUCCIÓN PARA EL LLM ---"));
        assertFalse(request.systemPrompt().contains("## INSTRUCCIÓN ESTRATÉGICA"));
    }

    @Test
    void execute_modoReporte_deberiaFallarSiFaltaElConsultingReport() {
        var ctx = context(false);
        ctx.decision(ConversationDecision.generateReport("contexto completo"));
        ctx.consultingReport(null);

        var exception = assertThrows(IllegalStateException.class, () -> stage.execute(ctx));
        assertEquals("consultingReport es obligatorio para responder en modo REPORT", exception.getMessage());
    }

    @Test
    void execute_conDirectiva_deberiaEnmarcarElPromptConLaDirectiva() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        ctx.turnDirective(directivaExploracion());
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var prompt = captor.getValue().systemPrompt();
        assertTrue(prompt.contains("## DIRECTIVA DE COMUNICACIÓN"));
        assertTrue(prompt.contains(ConversationPhase.EXPLORATION.name()));
        assertTrue(prompt.contains(CommunicationMode.QUESTION.name()));
        assertTrue(prompt.contains(String.valueOf(TurnConstraints.QUESTION_MAX_LENGTH)));
        assertTrue(prompt.contains("=== CONSULTING REPORT ==="));
    }

    @Test
    void execute_sinDirectiva_noDeberiaIncluirLaSeccionDeDirectiva() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        assertFalse(captor.getValue().systemPrompt().contains("## DIRECTIVA DE COMUNICACIÓN"));
    }

    @Test
    void execute_streaming_conDirectiva_deberiaMarcarResponseValidationAceptado() {
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just("¿Apuntás a empresas?"));

        var ctx = context(true);
        ctx.turnDirective(directivaExploracion());
        stage.execute(ctx);

        assertNotNull(ctx.aiResponseFlux());
        ctx.aiResponseFlux().blockLast();

        var validation = ctx.responseValidation();
        assertNotNull(validation);
        assertTrue(validation.accepted());
    }

    @Test
    void execute_streaming_conDirectiva_deberiaMarcarResponseValidationRechazado() {
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just("a".repeat(300) + "?"));

        var ctx = context(true);
        ctx.turnDirective(directivaExploracion());
        stage.execute(ctx);

        ctx.aiResponseFlux().blockLast();

        var validation = ctx.responseValidation();
        assertNotNull(validation);
        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.too_long"));
    }

    @Test
    void execute_streaming_sinDirectiva_noDeberiaMarcarResponseValidation() {
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just("hola"));

        var ctx = context(true);
        stage.execute(ctx);

        ctx.aiResponseFlux().blockLast();
        assertNull(ctx.responseValidation());
    }

    @Test
    void execute_streaming_modoReporte_sinDirectiva_deberiaUsarRestriccionesDeReporte() {
        when(aiResponder.respondStream(any(AIRequest.class)))
            .thenReturn(Flux.just("=== CONSULTING REPORT === resumen ejecutivo"));

        var ctx = context(true);
        ctx.decision(ConversationDecision.generateReport("contexto completo"));
        ctx.consultingReport(com.kinplatform.kin.reporting.report.model.ConsultingReport.empty());
        stage.execute(ctx);

        ctx.aiResponseFlux().blockLast();

        var validation = ctx.responseValidation();
        assertNotNull(validation);
        assertTrue(validation.accepted());
    }

    private TurnDirective directivaExploracion() {
        return new TurnDirective(
            ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK,
            AnalyzedDimension.PROBLEM, CommunicationMode.QUESTION, TurnConstraints.question());
    }

    private InterviewResult resultadoEntrevistaActiva() {
        var directive = InterviewDirective.of("q-sector", AnalyzedDimension.SECTOR,
            "sector y giro del negocio", AnswerRules.defaults());
        var state = InterviewState.empty(UUID.randomUUID())
            .withCurrent("q-sector").withPending(List.of("q-sector"));
        return InterviewResult.of(InterviewDecision.ask("q-sector", "Falta información"),
            directive, state, state.toProgress(5));
    }

    private InterviewResult resultadoEntrevistaCompleta() {
        var state = InterviewState.empty(UUID.randomUUID()).withComplete(true);
        return InterviewResult.of(InterviewDecision.report("completa"), null, state,
            state.toProgress(5));
    }

    @Test
    void execute_conEntrevistaActiva_deberiaUsarElPromptConversacionalConEntrevista() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿En qué rubro vas a operar?");

        var ctx = context(false);
        ctx.decision(ConversationDecision.ask(AnalyzedDimension.SECTOR, 9,
            "Entrevista estratégica: sector del negocio"));
        ctx.interviewResult(resultadoEntrevistaActiva());
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var prompt = captor.getValue().systemPrompt();
        assertTrue(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
        assertTrue(prompt.contains("sector y giro del negocio"));
        assertFalse(prompt.contains("=== CONSULTING REPORT ==="));
    }

    @Test
    void execute_conEntrevistaActiva_sobreDecididoReporte_deberiaSeguirUsandoElPromptConversacional() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿En qué rubro vas a operar?");

        var ctx = context(false);
        ctx.decision(ConversationDecision.generateReport("contexto completo"));
        ctx.consultingReport(com.kinplatform.kin.reporting.report.model.ConsultingReport.empty());
        ctx.interviewResult(resultadoEntrevistaActiva());
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var prompt = captor.getValue().systemPrompt();
        assertTrue(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
        assertFalse(prompt.contains("=== CONSULTING REPORT ==="));
    }

    @Test
    void execute_conEntrevistaCompleta_deberiaUsarElPromptDeReporte() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        ctx.decision(ConversationDecision.generateReport("contexto completo"));
        ctx.consultingReport(com.kinplatform.kin.reporting.report.model.ConsultingReport.empty());
        ctx.interviewResult(resultadoEntrevistaCompleta());
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var prompt = captor.getValue().systemPrompt();
        assertTrue(prompt.contains("=== CONSULTING REPORT ==="));
        assertFalse(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
    }

    @Test
    void execute_conEntrevistaVacia_deberiaIgnorarElResultadoYUsarLaDecision() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        ctx.decision(ConversationDecision.generateReport("contexto completo"));
        ctx.consultingReport(com.kinplatform.kin.reporting.report.model.ConsultingReport.empty());
        ctx.interviewResult(InterviewResult.empty());
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var prompt = captor.getValue().systemPrompt();
        assertTrue(prompt.contains("=== CONSULTING REPORT ==="));
        assertFalse(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
    }
}
