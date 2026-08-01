package com.kinplatform.kin;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KinMethodTest {

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    private KinMethod kinMethod;
    private InMemoryDomainEventBus eventBus;

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventBus = new InMemoryDomainEventBus();
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
            new com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter()
        ));
        var promptAssembler = new PromptAssembler(conversationBuilder, reportBuilder);
        var pipeline = new Pipeline(List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
            new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())),
            new RiskStage(new RiskEngine(
                List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
                RiskModel.defaultModel())),
            new OpportunityStage(new OpportunityEngine(
                List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                OpportunityModel.defaultModel())),
            new ReportStage(reportEngine()),
            new ConsultorStage(aiResponder, promptAssembler),
            new EventStage()
        ));
        kinMethod = new KinMethod(pipeline, eventBus, contextRepository);
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

    private KinMethodCommand command(String message) {
        return new KinMethodCommand(
            PROJECT_ID, USER_ID, message, List.of(),
            "Proyecto Test", "Descripción", "Software");
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
            .thenReturn(ProjectContext.restore(
                data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false));
    }

    @Test
    void execute_deberiaCorrerElPipelineCompleto_conContextoNuevoYCargarRespuesta() {
        stubNewContext();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("respuesta de KIN");

        var result = kinMethod.execute(command("el problema es que la gente pierde tiempo"));

        assertEquals("respuesta de KIN", result.aiResponse());
        assertEquals(com.kinplatform.kin.decision.ConversationDecision.Action.ASK, result.decision().action());
        assertNotNull(result.projectContext());
        assertEquals(1, result.projectContext().exchangeCount());
        verify(contextRepository).save(eq(PROJECT_ID), any(ProjectContext.class));

        assertTrue(result.events().stream().anyMatch(e -> e instanceof QuestionGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof com.kinplatform.kin.event.ConversationCompletedEvent));
        assertEquals(result.events(), eventBus.publishedEvents());
    }

    @Test
    void execute_deberiaGenerarInformeCuandoElContextoEstaCompleto() {
        stubFullContext();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = kinMethod.execute(command("generá el informe"));

        assertEquals(com.kinplatform.kin.decision.ConversationDecision.Action.REPORT, result.decision().action());
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
    void executeStream_deberiaDevolverElFluxDeTokensSinBloquear() {
        stubNewContext();
        when(aiResponder.respondStream(any(AIRequest.class)))
            .thenReturn(reactor.core.publisher.Flux.just("a", "b"));

        var flux = kinMethod.executeStream(command("hola"));
        assertEquals("ab", flux.reduce("", (acc, next) -> acc + next).block());

        verify(contextRepository).save(eq(PROJECT_ID), any(ProjectContext.class));
        assertFalse(eventBus.publishedEvents().isEmpty());
    }
}
