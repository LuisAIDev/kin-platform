package com.kinplatform.kin;

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
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.enrichment.FactRanker;
import com.kinplatform.kin.enrichment.stage.EnrichmentStage;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.interview.InMemoryInterviewRepository;
import com.kinplatform.kin.interview.engine.AnswerValidator;
import com.kinplatform.kin.interview.engine.InterviewBlueprint;
import com.kinplatform.kin.interview.engine.InterviewEngine;
import com.kinplatform.kin.interview.stage.InterviewStage;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.knowledge.stage.KnowledgeStage;
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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Construye el pipeline REAL de 13 etapas (FASE 9, E6) con todos sus motores
 * reales: Analyzer, Evaluator, Strategist, Interview (completa), Knowledge
 * (con fuente de datos), Enrichment, Scoring, Recommendation, Risk,
 * Opportunity, Report, Consultor (con el puerto {@link AIResponder}) y Events.
 * Solo los puertos de infraestructura (IA, contexto durable) se inyectan.
 */
public final class TestIntegrationPipeline {

    private TestIntegrationPipeline() {
    }

    public static Pipeline realPipeline(AIResponder aiResponder, UUID projectId) {
        return new Pipeline(stages(aiResponder, projectId));
    }

    public static Pipeline realPipeline(AIResponder aiResponder, UUID projectId, List<String> order) {
        return new Pipeline(stages(aiResponder, projectId).stream()
            .map(s -> record(s, order))
            .toList());
    }

    public static KinMethod realKinMethod(AIResponder aiResponder, ContextRepository contextRepository,
                                          DomainEventBus eventBus, UUID projectId) {
        return new KinMethod(realPipeline(aiResponder, projectId), eventBus, contextRepository);
    }

    public static ProjectContext fullContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        return ProjectContext.restore(data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false);
    }

    public static PipelineContext pipelineContext(UUID projectId, UUID userId) {
        var ctx = new PipelineContext(projectId, userId, "generá el informe", List.of(),
            "Proyecto Test", "Descripción", "Software");
        ctx.projectContext(fullContext());
        return ctx;
    }

    private static List<PipelineStage> stages(AIResponder aiResponder, UUID projectId) {
        return List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            new InterviewStage(new InterviewEngine(interviewBlueprint(), new AnswerValidator()),
                new InMemoryInterviewRepository(completeInterview(projectId))),
            knowledgeStage(),
            new EnrichmentStage(new EnrichmentEngine(new FactRanker())),
            new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
            new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())),
            new RiskStage(new RiskEngine(
                List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
                RiskModel.defaultModel())),
            new OpportunityStage(new OpportunityEngine(
                List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                OpportunityModel.defaultModel())),
            new ReportStage(reportEngine()),
            new ConsultorStage(aiResponder, promptAssembler(), new ResponseGuard()),
            new EventStage()
        );
    }

    private static InterviewBlueprint interviewBlueprint() {
        return new InterviewBlueprint(List.of(
            InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR, "sector del negocio", 1),
            InterviewQuestion.required("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", 2)));
    }

    private static InterviewState completeInterview(UUID projectId) {
        return InterviewState.empty(projectId)
            .withAnswered(Map.of(
                "q-sector", InterviewAnswer.of("q-sector", "Restaurante"),
                "q-revenue", InterviewAnswer.of("q-revenue", "Ventas directas")))
            .withPending(List.of())
            .withComplete(true);
    }

    private static KnowledgeStage knowledgeStage() {
        var candidate = new KnowledgeCandidate(
            "El mercado retail crece con demanda del consumidor. Dato verificado.",
            "src-1", "Fuente", "https://example.com/reporte",
            OffsetDateTime.now().minusDays(10), "application/json",
            Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
        var source = new CapturingSource(candidate);
        var validator = new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json", "text/plain"));
        return new KnowledgeStage(new KnowledgeEngine(
            new KnowledgeGateway(new SourceRegistry(List.of(source)), validator)));
    }

    private static ReportEngine reportEngine() {
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

    private static PromptAssembler promptAssembler() {
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
        return new PromptAssembler(conversationBuilder, reportBuilder);
    }

    private static PipelineStage record(PipelineStage delegate, List<String> order) {
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

    private static final class CapturingSource implements KnowledgeSource {
        private final KnowledgeCandidate candidate;

        private CapturingSource(KnowledgeCandidate candidate) {
            this.candidate = candidate;
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            return List.of(candidate);
        }
    }
}
