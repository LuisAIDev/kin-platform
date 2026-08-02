package com.kinplatform.kin.conversation;

import com.kinplatform.kin.KinMethod;
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
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewRepository;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.interview.InMemoryInterviewRepository;
import com.kinplatform.kin.interview.engine.AnswerValidator;
import com.kinplatform.kin.interview.engine.InterviewBlueprint;
import com.kinplatform.kin.interview.engine.InterviewEngine;
import com.kinplatform.kin.interview.stage.InterviewStage;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.knowledge.stage.KnowledgeStage;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.stage.AnalyzerStage;
import com.kinplatform.kin.pipeline.stage.ConsultorStage;
import com.kinplatform.kin.pipeline.stage.EvaluatorStage;
import com.kinplatform.kin.pipeline.stage.EventStage;
import com.kinplatform.kin.pipeline.stage.OpportunityStage;
import com.kinplatform.kin.pipeline.stage.RecommendationStage;
import com.kinplatform.kin.pipeline.stage.ReportStage;
import com.kinplatform.kin.pipeline.stage.RiskStage;
import com.kinplatform.kin.pipeline.stage.ScoringStage;
import com.kinplatform.kin.pipeline.stage.StrategistStage;
import com.kinplatform.kin.reporting.RecommendationEngine;
import com.kinplatform.kin.reporting.RecommendationModel;
import com.kinplatform.kin.reporting.opportunity.MarketOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.MonetizationOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.OpportunityEngine;
import com.kinplatform.kin.reporting.opportunity.OpportunityModel;
import com.kinplatform.kin.reporting.report.ReportAssemblers;
import com.kinplatform.kin.reporting.report.ReportEngine;
import com.kinplatform.kin.reporting.report.ReportModel;
import com.kinplatform.kin.reporting.report.assembler.ExecutiveSummaryAssembler;
import com.kinplatform.kin.reporting.report.assembler.FinancialSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.InnovationSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.MarketSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.NextStepsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.OpportunitiesSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.RecommendationsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ReportMetadataAssembler;
import com.kinplatform.kin.reporting.report.assembler.RisksSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ScoresSectionAssembler;
import com.kinplatform.kin.reporting.risk.BusinessRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.MarketRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.RiskEngine;
import com.kinplatform.kin.reporting.risk.RiskModel;
import com.kinplatform.kin.scoring.ScoringEngine;
import com.kinplatform.kin.scoring.ScoringModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integración E6 (ADR-015): verifica el flujo completo de la entrevista a través
 * del {@link ConversationOrchestrator}: persistencia entre turnos
 * ({@link InterviewRepository} → {@link InterviewStage}), gating de REPORT por
 * completitud (bloqueo del reporte mientras ASK) y enmarcado del prompt
 * {@code ## ENTREVISTA ESTRAT\u00C9GICA} en el {@code ConsultorStage}.
 */
@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorInterviewIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    private ConversationOrchestrator orchestrator;
    private InterviewRepository interviewRepository;

    @BeforeEach
    void setUp() {
        interviewRepository = new InMemoryInterviewRepository();
        var interviewBlueprint = new InterviewBlueprint(List.of(
            InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR,
                "sector y giro del negocio", 1),
            InterviewQuestion.required("q-revenue", AnalyzedDimension.REVENUE_MODEL,
                "modelo de ingresos", 2)));
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
            new ReportMetadataFormatter()
        ));
        var promptAssembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var gateway = new KnowledgeGateway(SourceRegistry.empty(), SourceValidator.strict());
        var pipeline = new Pipeline(List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            new InterviewStage(new InterviewEngine(interviewBlueprint, new AnswerValidator()),
                interviewRepository),
            new KnowledgeStage(new KnowledgeEngine(gateway)),
            new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
            new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())),
            new RiskStage(new RiskEngine(
                List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
                RiskModel.defaultModel())),
            new OpportunityStage(new OpportunityEngine(
                List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                OpportunityModel.defaultModel())),
            new ReportStage(reportEngine()),
            new ConsultorStage(aiResponder, promptAssembler, new ResponseGuard()),
            new EventStage()
        ));
        var kinMethod = new KinMethod(pipeline, new InMemoryDomainEventBus(), contextRepository);
        orchestrator = new ConversationOrchestrator(
            new HistoryWindow(), new DefaultTurnPolicy(), kinMethod,
            new ResponseGuard(), contextRepository);
    }

    private ReportEngine reportEngine() {
        var model = ReportModel.defaultModel();
        return new ReportEngine(new ReportAssemblers(
            new ExecutiveSummaryAssembler(),
            new ScoresSectionAssembler(),
            new RecommendationsSectionAssembler(),
            new RisksSectionAssembler(),
            new OpportunitiesSectionAssembler(),
            new FinancialSectionAssembler(),
            new MarketSectionAssembler(),
            new InnovationSectionAssembler(),
            new NextStepsSectionAssembler(model),
            new ReportMetadataAssembler(model)), model);
    }

    private ConversationTurn turn(String userMessage) {
        return new ConversationTurn(PROJECT_ID, USER_ID, userMessage, List.of(),
            "Proyecto Test", "Descripción", "Software");
    }

    private void stubContextoNuevo() {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(ProjectContext.fromProject("Proyecto Test", "Descripción", "Software"));
    }

    private void stubContextoCompleto() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(ProjectContext.restore(
                data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false));
    }

    private AIRequest capturarAIRequest() {
        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        return captor.getValue();
    }

    @Test
    void flujoEntrevista_primerTurno_deberiaFormularLaPreguntaYPersistirElEstado() {
        stubContextoNuevo();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿En qué rubro vas a operar?");

        var result = orchestrator.orchestrate(turn("es una app para reservas"));

        assertEquals(ConversationPhase.EXPLORATION, result.directive().phase());
        assertEquals(CommunicationMode.QUESTION, result.directive().communicationMode());
        assertEquals(ConversationDecision.Action.ASK, result.decision().action());
        assertNull(result.consultingReport());

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("## ENTREVISTA ESTRATÉGICA"));
        assertTrue(request.systemPrompt().contains("sector y giro del negocio"));

        var persisted = interviewRepository.find(PROJECT_ID).orElseThrow();
        assertEquals("q-sector", persisted.current());
    }

    @Test
    void flujoEntrevista_segundoTurno_deberiaProcesarLaRespuestaYAvanzar() {
        stubContextoNuevo();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿Cómo vas a generar ingresos?");
        interviewRepository.save(InterviewState.empty(PROJECT_ID).withCurrent("q-sector"));

        var result = orchestrator.orchestrate(turn("trabajamos en gastronomía"));

        assertEquals(ConversationDecision.Action.ASK, result.decision().action());
        assertNull(result.consultingReport());

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("## ENTREVISTA ESTRATÉGICA"));
        assertTrue(request.systemPrompt().contains("modelo de ingresos"));

        var persisted = interviewRepository.find(PROJECT_ID).orElseThrow();
        assertTrue(persisted.hasAnswered("q-sector"));
        assertEquals("q-revenue", persisted.current());
    }

    @Test
    void flujoEntrevista_turnoFinal_deberiaCompletarLaEntrevistaYGenerarElReporte() {
        stubContextoNuevo();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("Aquí tenés el informe de viabilidad.");
        interviewRepository.save(InterviewState.empty(PROJECT_ID)
            .withCurrent("q-revenue")
            .withAnswered(Map.of("q-sector", InterviewAnswer.of("q-sector", "Restaurante"))));

        var result = orchestrator.orchestrate(turn("cobramos por venta directa"));

        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertNotNull(result.consultingReport());

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("=== CONSULTING REPORT ==="));
        assertFalse(request.systemPrompt().contains("## ENTREVISTA ESTRATÉGICA"));

        var persisted = interviewRepository.find(PROJECT_ID).orElseThrow();
        assertTrue(persisted.complete());
        assertTrue(persisted.hasAnswered("q-revenue"));
    }

    @Test
    void flujoEntrevista_conContextoCompleto_deberiaBloquearElReporteMientrasAsk() {
        stubContextoCompleto();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿En qué rubro vas a operar?");

        var result = orchestrator.orchestrate(turn("es una app para reservas"));

        assertEquals(ConversationDecision.Action.ASK, result.decision().action());
        assertNull(result.consultingReport());

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("## ENTREVISTA ESTRATÉGICA"));
        assertFalse(request.systemPrompt().contains("Acción: Generar el INFORME DE VIABILIDAD completo"));
    }

    @Test
    void flujoEntrevista_conEntrevistaCompleta_deberiaGenerarElReporte() {
        stubContextoCompleto();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("Aquí tenés el informe de viabilidad.");
        interviewRepository.save(InterviewState.empty(PROJECT_ID).withComplete(true));

        var result = orchestrator.orchestrate(turn("generá el informe"));

        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertNotNull(result.consultingReport());

        var request = capturarAIRequest();
        assertTrue(request.systemPrompt().contains("=== CONSULTING REPORT ==="));
        assertFalse(request.systemPrompt().contains("## ENTREVISTA ESTRATÉGICA"));
    }

    @Test
    void flujoEntrevista_multiturno_deberiaUsarElRepositorioCompartidoEntreTurnos() {
        stubContextoNuevo();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿Pregunta?");

        var primero = orchestrator.orchestrate(turn("es una app para reservas"));
        var segundo = orchestrator.orchestrate(turn("trabajamos en gastronomía"));

        assertEquals(ConversationDecision.Action.ASK, primero.decision().action());
        assertEquals(ConversationDecision.Action.ASK, segundo.decision().action());

        var persisted = interviewRepository.find(PROJECT_ID).orElseThrow();
        assertTrue(persisted.hasAnswered("q-sector"));
        assertEquals("q-revenue", persisted.current());
    }
}
