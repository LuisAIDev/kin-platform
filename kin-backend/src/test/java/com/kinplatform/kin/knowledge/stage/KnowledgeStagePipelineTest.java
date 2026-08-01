package com.kinplatform.kin.knowledge.stage;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeStagePipelineTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AIResponder aiResponder;

    private KnowledgeStage knowledgeStage() {
        var gateway = new KnowledgeGateway(SourceRegistry.empty(), SourceValidator.strict());
        return new KnowledgeStage(new KnowledgeEngine(gateway));
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

    private Pipeline fullPipeline() {
        return new Pipeline(List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            knowledgeStage(),
            new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
            new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())),
            new RiskStage(new RiskEngine(
                List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
                RiskModel.defaultModel())),
            new OpportunityStage(new OpportunityEngine(
                List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                OpportunityModel.defaultModel())),
            new ReportStage(reportEngine()),
            new ConsultorStage(aiResponder, promptAssembler()),
            new EventStage()
        ));
    }

    private PromptAssembler promptAssembler() {
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
        return new PromptAssembler(conversationBuilder, reportBuilder);
    }

    private PipelineContext context(ProjectContext projectContext) {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "generá el informe", List.of(),
            "Proyecto Test", "Descripción", "Software");
        ctx.projectContext(projectContext);
        return ctx;
    }

    private ProjectContext fullContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        return ProjectContext.restore(data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false);
    }

    @Test
    void pipeline_deberiaEjecutarKnowledgeStage_conProjectContext() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("respuesta");

        var result = fullPipeline().execute(context(fullContext()));

        assertNotNull(result.knowledgeResult());
        assertTrue(result.knowledgeResult().isEmpty());
        assertEquals(0, result.knowledgeResult().factCount());
        assertTrue(result.engineResults().containsKey(KnowledgeEngine.GENERATOR_NAME));
    }

    @Test
    void pipeline_deberiaOmitirKnowledgeStage_cuandoNoHayProjectContext() {
        var pipeline = new Pipeline(List.of(knowledgeStage()));
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "hola", List.of(),
            "T", "D", "C");

        var result = pipeline.execute(ctx);

        assertNull(result.knowledgeResult());
    }

    @Test
    void pipeline_deberiaEjecutarKnowledgeStageConElOrdenOficial() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var order = new ArrayList<String>();
        var pipeline = new Pipeline(List.of(
            record(new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()), order),
            record(new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())), order),
            record(new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))), order),
            record(knowledgeStage(), order),
            record(new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())), order),
            record(new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())), order),
            record(new RiskStage(new RiskEngine(
                List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
                RiskModel.defaultModel())), order),
            record(new OpportunityStage(new OpportunityEngine(
                List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                OpportunityModel.defaultModel())), order),
            record(new ReportStage(reportEngine()), order),
            record(new ConsultorStage(aiResponder, promptAssembler()), order),
            record(new EventStage(), order)
        ));

        pipeline.execute(context(fullContext()));

        assertEquals(List.of("Analizador", "Evaluador", "Estratega", "Conocimiento",
            "Scoring", "Recomendaciones", "Riesgos", "Oportunidades", "Reporte",
            "Consultor", "Eventos"), order);
    }

    @Test
    void pipeline_deberiaDejarKnowledgeResultDisponibleParaReportEngine() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = fullPipeline().execute(context(fullContext()));

        assertNotNull(result.knowledgeResult());
        assertNotNull(result.consultingReport());
        assertEquals(com.kinplatform.kin.decision.ConversationDecision.Action.REPORT,
            result.decision().action());
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.knowledgeResult().generatedBy());
        assertNotNull(result.engineResult(KnowledgeEngine.GENERATOR_NAME));
    }

    @Test
    void pipeline_deberiaDejarKnowledgeResultDisponibleParaConsultor() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = fullPipeline().execute(context(fullContext()));

        assertNotNull(result.aiResponse());
        assertFalse(result.aiResponse().isBlank());
        assertNotNull(result.knowledgeResult());
        assertNotNull(result.consultingReport());
    }

    @Test
    void pipeline_deberiaCompletarElPipelineConKnowledgeStageIntegrado() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = fullPipeline().execute(context(fullContext()));

        assertNotNull(result.scoreResult());
        assertNotNull(result.recommendationResult());
        assertNotNull(result.riskResult());
        assertNotNull(result.opportunityResult());
        assertNotNull(result.consultingReport());
        assertNotNull(result.knowledgeResult());
        assertEquals(10, result.consultingReport().metadata().sectionsIncluded().size());
        assertTrue(result.completed());
        assertFalse(result.events().isEmpty());
    }

    private PipelineStage record(PipelineStage delegate, List<String> order) {
        return new PipelineStage() {
            @Override
            public String name() {
                return delegate.name();
            }

            @Override
            public boolean supports(PipelineContext context) {
                return delegate.supports(context);
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                order.add(delegate.name());
                return delegate.execute(context);
            }
        };
    }
}
