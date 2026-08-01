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
import com.kinplatform.kin.decision.ConversationDecision;
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
}
