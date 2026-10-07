package com.kinplatform.common.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.platform.method.KinMethod;
import com.kinplatform.common.ai.AIRequest;
import com.kinplatform.common.ai.AIResponder;
import com.kinplatform.common.ai.PromptAssembler;
import com.kinplatform.common.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.common.ai.prompt.ReportPromptBuilder;
import com.kinplatform.common.ai.prompt.formatter.ExecutiveSummaryFormatter;
import com.kinplatform.common.ai.prompt.formatter.FinancialSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.InnovationSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.MarketSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.NextStepsSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.OpportunitiesSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.RecommendationsSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.ReportMetadataFormatter;
import com.kinplatform.common.ai.prompt.formatter.RisksSectionFormatter;
import com.kinplatform.common.ai.prompt.formatter.ScoresSectionFormatter;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.CompletenessEvaluator;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.EvaluationPolicies;
import com.kinplatform.common.context.ExplorationPriority;
import com.kinplatform.common.context.Message;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.context.strategy.ConversationStrategist;
import com.kinplatform.common.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.common.conversation.history.HistoryWindow;
import com.kinplatform.common.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.common.conversation.validation.ResponseGuard;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.common.event.QuestionGeneratedEvent;
import com.kinplatform.common.event.ReportGeneratedEvent;
import com.kinplatform.common.pipeline.Pipeline;
import com.kinplatform.common.pipeline.stage.AnalyzerStage;
import com.kinplatform.common.pipeline.stage.ConsultorStage;
import com.kinplatform.common.pipeline.stage.EvaluatorStage;
import com.kinplatform.common.pipeline.stage.EventStage;
import com.kinplatform.common.pipeline.stage.OpportunityStage;
import com.kinplatform.common.pipeline.stage.RecommendationStage;
import com.kinplatform.common.pipeline.stage.ReportStage;
import com.kinplatform.common.pipeline.stage.RiskStage;
import com.kinplatform.common.pipeline.stage.ScoringStage;
import com.kinplatform.common.pipeline.stage.StrategistStage;
import com.kinplatform.platform.reporting.RecommendationEngine;
import com.kinplatform.platform.reporting.RecommendationModel;
import com.kinplatform.platform.reporting.opportunity.MarketOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.MonetizationOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.OpportunityEngine;
import com.kinplatform.platform.reporting.opportunity.OpportunityModel;
import com.kinplatform.platform.reporting.report.ReportAssemblers;
import com.kinplatform.platform.reporting.report.ReportEngine;
import com.kinplatform.platform.reporting.report.ReportModel;
import com.kinplatform.platform.reporting.report.assembler.ExecutiveSummaryAssembler;
import com.kinplatform.platform.reporting.report.assembler.FinancialSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.InnovationSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.MarketSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.NextStepsSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.OpportunitiesSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.RecommendationsSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.ReportMetadataAssembler;
import com.kinplatform.platform.reporting.report.assembler.RisksSectionAssembler;
import com.kinplatform.platform.reporting.report.assembler.ScoresSectionAssembler;
import com.kinplatform.platform.reporting.risk.BusinessRiskAnalyzer;
import com.kinplatform.platform.reporting.risk.MarketRiskAnalyzer;
import com.kinplatform.platform.reporting.risk.RiskEngine;
import com.kinplatform.platform.reporting.risk.RiskModel;
import com.kinplatform.platform.scoring.ScoringEngine;
import com.kinplatform.platform.scoring.ScoringModel;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorPipelineIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    private ConversationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        var conversationBuilder = new ConversationPromptBuilder();
        var reportBuilder = new ReportPromptBuilder(List.of(
                new ExecutiveSummaryFormatter(),
                new ScoresSectionFormatter(),
                new RecommendationsSectionFormatter(),
                new RisksSectionFormatter(),
                new OpportunitiesSectionFormatter(),
                new FinancialSectionFormatter(),
                new MarketSectionFormatter(),
                new InnovationSectionFormatter(),
                new NextStepsSectionFormatter(),
                new ReportMetadataFormatter()));
        var promptAssembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var pipeline = new Pipeline(List.of(
                new AnalyzerStage((message, ctx) -> com.kinplatform.common.context.AnalysisResult.empty()),
                new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
                new StrategistStage(new ConversationStrategist(
                        new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
                new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
                new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())),
                new RiskStage(new RiskEngine(
                        List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()), RiskModel.defaultModel())),
                new OpportunityStage(new OpportunityEngine(
                        List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                        OpportunityModel.defaultModel())),
                new ReportStage(reportEngine()),
                new ConsultorStage(aiResponder, promptAssembler, new ResponseGuard()),
                new EventStage()));
        var kinMethod = new KinMethod(pipeline, contextRepository);
        orchestrator = new ConversationOrchestrator(
                new HistoryWindow(), new DefaultTurnPolicy(), kinMethod, new ResponseGuard(), contextRepository);
    }

    private ReportEngine reportEngine() {
        var model = ReportModel.defaultModel();
        return new ReportEngine(
                new ReportAssemblers(
                        new ExecutiveSummaryAssembler(),
                        new ScoresSectionAssembler(),
                        new RecommendationsSectionAssembler(),
                        new RisksSectionAssembler(),
                        new OpportunitiesSectionAssembler(),
                        new FinancialSectionAssembler(),
                        new MarketSectionAssembler(),
                        new InnovationSectionAssembler(),
                        new NextStepsSectionAssembler(model),
                        new ReportMetadataAssembler(model)),
                model);
    }

    private ConversationTurn turn() {
        return new ConversationTurn(
                PROJECT_ID,
                USER_ID,
                "el problema es que la gente pierde tiempo",
                List.of(),
                "Proyecto Test",
                "Descripción",
                "Software");
    }

    private void stubNuevoContexto() {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
                .thenReturn(ProjectContext.fromProject("Proyecto Test", "Descripción", "Software"));
    }

    private void stubContextoCompleto() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
                .thenReturn(ProjectContext.restore(data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false));
    }

    private AIRequest capturarAIRequest() {
        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        return captor.getValue();
    }

    @Test
    void flujoConversacion_deberiaEnmarcarElPromptConLaDirectivaYValidar() {
        stubNuevoContexto();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿Apuntás a consumidores o a empresas?");

        var result = orchestrator.orchestrate(turn());

        assertEquals(ConversationPhase.EXPLORATION, result.directive().phase());
        assertEquals(ConversationDecision.Action.ASK, result.directive().action());
        assertEquals(CommunicationMode.QUESTION, result.directive().communicationMode());
        assertTrue(result.validation().accepted());

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("## DIRECTIVA DE COMUNICACIÓN"));
        assertTrue(request.systemPrompt().contains(ConversationPhase.EXPLORATION.name()));
        assertTrue(request.systemPrompt().contains(CommunicationMode.QUESTION.name()));

        assertTrue(result.events().stream().anyMatch(e -> e instanceof QuestionGeneratedEvent));
        assertFalse(result.events().isEmpty());
    }

    @Test
    void flujoReporte_deberiaGenerarElReporteYNoEnmarcarLaDirectivaEnElPromptDeReporte() {
        stubContextoCompleto();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("Aquí tenés el informe de viabilidad.");

        var result = orchestrator.orchestrate(turn());

        assertNotNull(result.consultingReport());
        assertEquals("ReportEngine", result.consultingReport().generatedBy());
        assertEquals(10, result.consultingReport().metadata().sectionsIncluded().size());
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ReportGeneratedEvent));

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("=== CONSULTING REPORT ==="));
        assertFalse(request.systemPrompt().contains("## DIRECTIVA DE COMUNICACIÓN"));

        assertEquals(ConversationPhase.EXPLORATION, result.directive().phase());
        assertEquals(ConversationDecision.Action.ASK, result.directive().action());
    }

    @Test
    void flujoConversacion_streaming_deberiaEnmarcarElPromptYDevolverLosTokens() {
        stubNuevoContexto();
        when(aiResponder.respondStream(any(AIRequest.class)))
                .thenReturn(Flux.just("¿Apuntás a ", "consumidores o a empresas?"));

        var flux = orchestrator.orchestrateStream(turn());

        assertEquals(
                "¿Apuntás a consumidores o a empresas?",
                flux.reduce("", (acc, next) -> acc + next).block());

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respondStream(captor.capture());
        assertTrue(captor.getValue().systemPrompt().contains("## DIRECTIVA DE COMUNICACIÓN"));

        var outcome = orchestrator.orchestrateStreamWithOutcome(turn());
        assertFalse(outcome.events().isEmpty());
        assertTrue(outcome.events().stream()
                .anyMatch(e -> e instanceof com.kinplatform.common.event.ConversationCompletedEvent));
    }

    @Test
    void flujoStreaming_conRespuestaMayorA6000Caracteres_deberiaEntregarTodoSinErroresNiMensajesTecnicos() {
        stubNuevoContexto();

        String parte1 = "## Dónde podría quedar bien tu restaurante\n"
                + "- Calle del Arsenal\n- Plaza Santo Domingo\n- Callejón Ancho\n- Calle de la Mantilla\n"
                + "Estas zonas combinan tránsito turístico y peatonal constante, ideales para un restaurante.\n";
        String respuesta1 = parte1 + "x".repeat(6000);
        assertTrue(respuesta1.length() > 6000);
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just(respuesta1));

        var turno1 = new ConversationTurn(
                PROJECT_ID,
                USER_ID,
                "¿Dónde puedo ubicar mi restaurante?",
                List.of(),
                "Proyecto Test",
                "Descripción",
                "Software");
        String contenido1 = orchestrator
                .orchestrateStream(turno1)
                .reduce("", (acc, next) -> acc + next)
                .block();

        assertEquals(respuesta1, contenido1);
        assertFalse(contenido1.contains("No pude generar"));
        assertFalse(contenido1.contains("response.too_long"));
        assertFalse(contenido1.contains("Motivo"));

        // Turno 2: el usuario pulsa "La respuesta es extensa. ¿Deseas que continúe?"
        // -> se envía "Continúa, por favor." y la IA produce SOLO la continuación.
        String respuesta2 = "## Continuación\n"
                + "Ubicaciones alternativas: Centro Histórico, Getsemaní y Bocagrande.\n"
                + "- Avenida San Martín\n- Parque de la Marina\n- Castillo San Felipe\n";
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just(respuesta2));

        var historial = List.of(Message.user("¿Dónde puedo ubicar mi restaurante?"), Message.assistant(contenido1));
        var turno2 = new ConversationTurn(
                PROJECT_ID, USER_ID, "Continúa, por favor.", historial, "Proyecto Test", "Descripción", "Software");
        String contenido2 = orchestrator
                .orchestrateStream(turno2)
                .reduce("", (acc, next) -> acc + next)
                .block();

        assertEquals(respuesta2, contenido2);
        assertFalse(contenido2.contains("No pude generar"));
        assertFalse(contenido2.contains("response.too_long"));
        assertFalse(contenido2.contains("Motivo"));
        assertEquals(respuesta1, contenido1);
    }
}











