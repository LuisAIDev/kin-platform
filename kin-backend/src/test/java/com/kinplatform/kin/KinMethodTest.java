package com.kinplatform.kin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.CompletenessEvaluator;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.EvaluationPolicies;
import com.kinplatform.common.context.ExplorationPriority;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.context.strategy.ConversationStrategist;
import com.kinplatform.common.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
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

@ExtendWith(MockitoExtension.class)
class KinMethodTest {

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    private KinMethod kinMethod;

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var conversationBuilder = new com.kinplatform.kin.ai.prompt.ConversationPromptBuilder();
        var reportBuilder = new com.kinplatform.kin.ai.prompt.ReportPromptBuilder(List.of(
                new com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter(),
                new com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter()));
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
                new ConsultorStage(aiResponder, promptAssembler),
                new EventStage()));
        kinMethod = new KinMethod(pipeline, contextRepository);
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

    private KinMethodCommand command(String message) {
        return new KinMethodCommand(
                PROJECT_ID, USER_ID, message, List.of(), "Proyecto Test", "Descripción", "Software");
    }

    private KinMethodCommand command(String message, TurnDirective directive) {
        return new KinMethodCommand(
                PROJECT_ID, USER_ID, message, List.of(), "Proyecto Test", "Descripción", "Software", directive);
    }

    private void stubNewContext() {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
                .thenReturn(ProjectContext.fromProject("Proyecto Test", "Descripción", "Software"));
    }

    private void stubFullContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
                .thenReturn(ProjectContext.restore(data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false));
    }

    @Test
    void execute_deberiaCorrerElPipelineCompleto_conContextoNuevoYCargarRespuesta() {
        stubNewContext();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("respuesta de KIN");

        var result = kinMethod.execute(command("el problema es que la gente pierde tiempo"));

        assertEquals("respuesta de KIN", result.aiResponse());
        assertEquals(
                com.kinplatform.common.decision.ConversationDecision.Action.ASK,
                result.decision().action());
        assertNotNull(result.projectContext());
        assertEquals(1, result.projectContext().exchangeCount());
        verify(contextRepository).save(eq(PROJECT_ID), any(ProjectContext.class));

        assertTrue(result.events().stream().anyMatch(e -> e instanceof QuestionGeneratedEvent));
        assertTrue(result.events().stream()
                .anyMatch(e -> e instanceof com.kinplatform.kin.event.ConversationCompletedEvent));
    }

    @Test
    void execute_deberiaGenerarInformeCuandoElContextoEstaCompleto() {
        stubFullContext();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = kinMethod.execute(command("generá el informe"));

        assertEquals(
                com.kinplatform.common.decision.ConversationDecision.Action.REPORT,
                result.decision().action());
        assertNotNull(result.score());
        assertEquals("ScoringEngine", result.score().generatedBy());
        assertTrue(result.score().totalScore() > 0);

        assertTrue(result.events().stream().anyMatch(e -> e instanceof ReportGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ScoreCalculatedEvent));

        assertNotNull(result.consultingReport());
        assertEquals("ReportEngine", result.consultingReport().generatedBy());
        assertEquals(10, result.consultingReport().metadata().sectionsIncluded().size());

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        assertTrue(captor.getValue().systemPrompt().contains("=== CONSULTING REPORT ==="));
        assertTrue(captor.getValue().systemPrompt().contains("--- INSTRUCCIÓN PARA EL LLM ---"));
        assertFalse(captor.getValue().systemPrompt().contains("## INSTRUCCIÓN ESTRATÉGICA"));
    }

    @Test
    void executeStream_deberiaDevolverElFluxDeTokensYLosEventosSinPublicarlos() {
        stubNewContext();
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(reactor.core.publisher.Flux.just("a", "b"));

        var outcome = kinMethod.executeStreamWithOutcome(command("hola"));
        assertEquals(
                "ab", outcome.safeFlux().reduce("", (acc, next) -> acc + next).block());

        verify(contextRepository).save(eq(PROJECT_ID), any(ProjectContext.class));
        assertFalse(outcome.result().events().isEmpty());
        assertTrue(outcome.result().events().stream()
                .anyMatch(e -> e instanceof com.kinplatform.kin.event.ConversationCompletedEvent));
    }

    @Test
    void execute_deberiaPropagarLaDirectivaDelComandoAlPrompt() {
        stubNewContext();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿Apuntás a empresas?");
        var directive = new TurnDirective(
                ConversationPhase.EXPLORATION,
                com.kinplatform.common.decision.ConversationDecision.Action.ASK,
                AnalyzedDimension.PROBLEM,
                CommunicationMode.QUESTION,
                TurnConstraints.question());

        kinMethod.execute(command("hola", directive));

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var prompt = captor.getValue().systemPrompt();
        assertTrue(prompt.contains("## DIRECTIVA DE COMUNICACIÓN"));
        assertTrue(prompt.contains(ConversationPhase.EXPLORATION.name()));
        assertTrue(prompt.contains(CommunicationMode.QUESTION.name()));
        assertTrue(prompt.contains(String.valueOf(TurnConstraints.QUESTION_MAX_LENGTH)));
    }

    @Test
    void execute_sinDirectiva_deberiaMantenerElPromptSinDirectiva() {
        stubNewContext();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿Pregunta?");

        kinMethod.execute(command("hola"));

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        assertFalse(captor.getValue().systemPrompt().contains("## DIRECTIVA DE COMUNICACIÓN"));
    }
}





