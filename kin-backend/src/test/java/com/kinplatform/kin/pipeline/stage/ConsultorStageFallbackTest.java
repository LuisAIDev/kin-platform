package com.kinplatform.kin.pipeline.stage;

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
import com.kinplatform.kin.conversation.ResponseFallback;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultorStageFallbackTest {

    @Mock
    private AIResponder aiResponder;

    private PromptAssembler promptAssembler;
    private final ResponseGuard responseGuard = new ResponseGuard();

    @BeforeEach
    void setUp() {
        promptAssembler = new PromptAssembler(new ConversationPromptBuilder(), new ReportPromptBuilder(List.of(
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
        )));
    }

    private ConsultorStage stage(ResponseFallback fallback) {
        return new ConsultorStage(aiResponder, promptAssembler, responseGuard, fallback);
    }

    private PipelineContext streamingContext() {
        var ctx = new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "hola", List.of(),
            "Mi App", "App de gestión", "Software");
        var projectContext = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");
        projectContext.attachDecision(decision);
        ctx.projectContext(projectContext);
        ctx.decision(decision);
        ctx.streaming(true);
        ctx.turnDirective(new TurnDirective(
            ConversationPhase.EXPLORATION, ConversationDecision.Action.ASK,
            AnalyzedDimension.PROBLEM, CommunicationMode.QUESTION, TurnConstraints.question()));
        return ctx;
    }

    @Test
    void validacionAceptada_deberiaNoReintentar() {
        when(aiResponder.respondStream(any())).thenReturn(Flux.just("¿Pregunta?"));

        var ctx = streamingContext();
        stage(new ResponseFallback(List.of("segura"), 2)).execute(ctx);

        StepVerifier.create(ctx.aiResponseFlux()).expectNext("¿Pregunta?").verifyComplete();
        assertTrue(ctx.responseValidation().accepted());
        verify(aiResponder, times(1)).respondStream(any());
    }

    @Test
    void validacionRechazada_sinRetry_deberiaNoReintentarYConservarLaValidacion() {
        when(aiResponder.respondStream(any())).thenReturn(Flux.just("¿A? ¿B?"));

        var ctx = streamingContext();
        stage(new ResponseFallback(List.of("segura"), 0)).execute(ctx);

        StepVerifier.create(ctx.aiResponseFlux()).expectNext("¿A? ¿B?").verifyComplete();
        assertFalse(ctx.responseValidation().accepted());
        verify(aiResponder, times(1)).respondStream(any());
    }

    @Test
    void validacionRechazada_conRetry_deberiaReintentarConFlujoNuevo() {
        when(aiResponder.respondStream(any()))
            .thenReturn(Flux.just("¿A? ¿B?"))
            .thenReturn(Flux.just("¿Pregunta válida?"));

        var ctx = streamingContext();
        stage(new ResponseFallback(List.of("segura"), 2)).execute(ctx);

        StepVerifier.create(ctx.aiResponseFlux())
            .expectNext("¿A? ¿B?", "¿Pregunta válida?")
            .verifyComplete();
        assertTrue(ctx.responseValidation().accepted());
        verify(aiResponder, times(2)).respondStream(any());
    }

    @Test
    void validacionRechazada_conRetryAgotado_deberiaReintentarHastaAgotar() {
        when(aiResponder.respondStream(any())).thenReturn(Flux.just("¿A? ¿B?"));

        var ctx = streamingContext();
        stage(new ResponseFallback(List.of("segura"), 2)).execute(ctx);

        StepVerifier.create(ctx.aiResponseFlux())
            .expectNext("¿A? ¿B?", "¿A? ¿B?", "¿A? ¿B?")
            .verifyComplete();
        assertFalse(ctx.responseValidation().accepted());
        verify(aiResponder, times(3)).respondStream(any());
    }
}
