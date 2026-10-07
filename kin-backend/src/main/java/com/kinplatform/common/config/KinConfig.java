package com.kinplatform.common.config;

import com.kinplatform.common.ai.interview.adapter.JpaInterviewRepository;
import com.kinplatform.common.ai.knowledge.adapter.KinKnowledgeProperties;
import com.kinplatform.common.ai.provider.AIProvider;
import com.kinplatform.common.ai.provider.ProviderRouter;
import com.kinplatform.common.method.KinMethod;
import com.kinplatform.common.ai.AIResponder;
import com.kinplatform.common.ai.PromptAssembler;
import com.kinplatform.common.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.common.ai.prompt.ReportPromptBuilder;
import com.kinplatform.common.ai.prompt.SectionFormatter;
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
import com.kinplatform.common.ai.prompt.formatter.SourcesSectionFormatter;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.CompletenessEvaluator;
import com.kinplatform.common.context.ContextAnalyzerPort;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.EvaluationPolicies;
import com.kinplatform.common.context.ExplorationPriority;
import com.kinplatform.common.context.ProjectContextSyncPort;
import com.kinplatform.common.context.strategy.ConversationStrategist;
import com.kinplatform.common.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.common.conversation.ConversationOrchestrator;
import com.kinplatform.common.conversation.ResponseFallback;
import com.kinplatform.common.conversation.history.HistoryWindow;
import com.kinplatform.common.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.common.conversation.validation.ResponseGuard;
import com.kinplatform.common.engine.DomainEngine;
import com.kinplatform.common.engine.EngineExecutor;
import com.kinplatform.common.engine.EngineRegistry;
import com.kinplatform.common.enrichment.EnrichmentEngine;
import com.kinplatform.common.enrichment.FactRanker;
import com.kinplatform.common.enrichment.stage.EnrichmentStage;
import com.kinplatform.platform.enterprise.application.EnterprisePipelineResultStore;
import com.kinplatform.platform.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.kin.health.differential.stage.DifferentialStage;
import com.kinplatform.kin.health.triage.stage.TriageStage;
import com.kinplatform.common.interview.InterviewQuestion;
import com.kinplatform.common.interview.InterviewRepository;
import com.kinplatform.common.interview.engine.AnswerValidator;
import com.kinplatform.common.interview.engine.InterviewBlueprint;
import com.kinplatform.common.interview.engine.InterviewEngine;
import com.kinplatform.common.interview.stage.InterviewStage;
import com.kinplatform.common.knowledge.KnowledgeRepository;
import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.deduplication.DeduplicationEngine;
import com.kinplatform.common.knowledge.deduplication.DeduplicationStage;
import com.kinplatform.common.knowledge.deduplication.DeduplicationStrategy;
import com.kinplatform.common.knowledge.deduplication.ExactMatchStrategy;
import com.kinplatform.common.knowledge.deduplication.FuzzyMatchStrategy;
import com.kinplatform.common.knowledge.deduplication.SemanticMatchStrategy;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import com.kinplatform.common.knowledge.engine.KnowledgeGateway;
import com.kinplatform.common.knowledge.engine.SourceRegistry;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import com.kinplatform.common.knowledge.stage.KnowledgeStage;
import com.kinplatform.common.pipeline.Pipeline;
import com.kinplatform.common.pipeline.resilience.StagePolicy;
import com.kinplatform.common.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.common.pipeline.resilience.StageTimeoutConfig;
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
import com.kinplatform.platform.reporting.opportunity.AutomationOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.CompetitiveOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.FinancialOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.InnovationOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.MarketOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.MonetizationOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.OpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.OpportunityEngine;
import com.kinplatform.platform.reporting.opportunity.OpportunityModel;
import com.kinplatform.platform.reporting.opportunity.ScalabilityOpportunityAnalyzer;
import com.kinplatform.platform.reporting.opportunity.TechnologicalOpportunityAnalyzer;
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
import com.kinplatform.platform.reporting.risk.FinancialRiskAnalyzer;
import com.kinplatform.platform.reporting.risk.MarketRiskAnalyzer;
import com.kinplatform.platform.reporting.risk.RiskAnalyzer;
import com.kinplatform.platform.reporting.risk.RiskEngine;
import com.kinplatform.platform.reporting.risk.RiskModel;
import com.kinplatform.platform.reporting.risk.TechnicalRiskAnalyzer;
import com.kinplatform.platform.scoring.ScoringEngine;
import com.kinplatform.platform.scoring.ScoringModel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KinConfig {

    @Bean
    public ScoringModel scoringModel() {
        return ScoringModel.defaultModel();
    }

    @Bean
    public ScoringEngine scoringEngine(ScoringModel scoringModel) {
        return new ScoringEngine(scoringModel);
    }

    @Bean
    public ExecutiveSummaryFormatter executiveSummaryFormatter() {
        return new ExecutiveSummaryFormatter();
    }

    @Bean
    public ScoresSectionFormatter scoresSectionFormatter() {
        return new ScoresSectionFormatter();
    }

    @Bean
    public RecommendationsSectionFormatter recommendationsSectionFormatter() {
        return new RecommendationsSectionFormatter();
    }

    @Bean
    public RisksSectionFormatter risksSectionFormatter() {
        return new RisksSectionFormatter();
    }

    @Bean
    public OpportunitiesSectionFormatter opportunitiesSectionFormatter() {
        return new OpportunitiesSectionFormatter();
    }

    @Bean
    public FinancialSectionFormatter financialSectionFormatter() {
        return new FinancialSectionFormatter();
    }

    @Bean
    public MarketSectionFormatter marketSectionFormatter() {
        return new MarketSectionFormatter();
    }

    @Bean
    public InnovationSectionFormatter innovationSectionFormatter() {
        return new InnovationSectionFormatter();
    }

    @Bean
    public NextStepsSectionFormatter nextStepsSectionFormatter() {
        return new NextStepsSectionFormatter();
    }

    @Bean
    public ReportMetadataFormatter reportMetadataFormatter() {
        return new ReportMetadataFormatter();
    }

    @Bean
    public SourcesSectionFormatter sourcesSectionFormatter() {
        return new SourcesSectionFormatter();
    }

    @Bean
    public AnalyzerStage analyzerStage(ContextAnalyzerPort analyzer) {
        return new AnalyzerStage(analyzer);
    }

    @Bean
    public EvaluatorStage evaluatorStage(CompletenessEvaluator evaluator) {
        return new EvaluatorStage(evaluator);
    }

    @Bean
    public StrategistStage strategistStage(ConversationStrategist strategist) {
        return new StrategistStage(strategist);
    }

    @Bean
    public ConsultorStage consultorStage(
            AIResponder aiResponder, PromptAssembler promptAssembler, ResponseGuard responseGuard) {
        return new ConsultorStage(aiResponder, promptAssembler, responseGuard);
    }

    @Bean
    public ScoringStage scoringStage(ScoringEngine scoringEngine) {
        return new ScoringStage(scoringEngine);
    }

    @Bean
    public RecommendationModel recommendationModel() {
        return RecommendationModel.defaultModel();
    }

    @Bean
    public RecommendationEngine recommendationEngine(RecommendationModel recommendationModel) {
        return new RecommendationEngine(recommendationModel);
    }

    @Bean
    public RecommendationStage recommendationStage(RecommendationEngine recommendationEngine) {
        return new RecommendationStage(recommendationEngine);
    }

    @Bean
    public RiskModel riskModel() {
        return RiskModel.defaultModel();
    }

    @Bean
    public BusinessRiskAnalyzer businessRiskAnalyzer() {
        return new BusinessRiskAnalyzer();
    }

    @Bean
    public TechnicalRiskAnalyzer technicalRiskAnalyzer() {
        return new TechnicalRiskAnalyzer();
    }

    @Bean
    public FinancialRiskAnalyzer financialRiskAnalyzer() {
        return new FinancialRiskAnalyzer();
    }

    @Bean
    public MarketRiskAnalyzer marketRiskAnalyzer() {
        return new MarketRiskAnalyzer();
    }

    @Bean
    public RiskEngine riskEngine(List<RiskAnalyzer> analyzers, RiskModel riskModel) {
        return new RiskEngine(analyzers, riskModel);
    }

    @Bean
    public RiskStage riskStage(RiskEngine riskEngine) {
        return new RiskStage(riskEngine);
    }

    @Bean
    public OpportunityModel opportunityModel() {
        return OpportunityModel.defaultModel();
    }

    @Bean
    public MarketOpportunityAnalyzer marketOpportunityAnalyzer() {
        return new MarketOpportunityAnalyzer();
    }

    @Bean
    public InnovationOpportunityAnalyzer innovationOpportunityAnalyzer() {
        return new InnovationOpportunityAnalyzer();
    }

    @Bean
    public TechnologicalOpportunityAnalyzer technologicalOpportunityAnalyzer() {
        return new TechnologicalOpportunityAnalyzer();
    }

    @Bean
    public FinancialOpportunityAnalyzer financialOpportunityAnalyzer() {
        return new FinancialOpportunityAnalyzer();
    }

    @Bean
    public CompetitiveOpportunityAnalyzer competitiveOpportunityAnalyzer() {
        return new CompetitiveOpportunityAnalyzer();
    }

    @Bean
    public ScalabilityOpportunityAnalyzer scalabilityOpportunityAnalyzer() {
        return new ScalabilityOpportunityAnalyzer();
    }

    @Bean
    public AutomationOpportunityAnalyzer automationOpportunityAnalyzer() {
        return new AutomationOpportunityAnalyzer();
    }

    @Bean
    public MonetizationOpportunityAnalyzer monetizationOpportunityAnalyzer() {
        return new MonetizationOpportunityAnalyzer();
    }

    @Bean
    public OpportunityEngine opportunityEngine(List<OpportunityAnalyzer> analyzers, OpportunityModel opportunityModel) {
        return new OpportunityEngine(analyzers, opportunityModel);
    }

    @Bean
    public OpportunityStage opportunityStage(OpportunityEngine opportunityEngine) {
        return new OpportunityStage(opportunityEngine);
    }

    @Bean
    public ReportModel reportModel() {
        return ReportModel.defaultModel();
    }

    @Bean
    public ExecutiveSummaryAssembler executiveSummaryAssembler() {
        return new ExecutiveSummaryAssembler();
    }

    @Bean
    public ScoresSectionAssembler scoresSectionAssembler() {
        return new ScoresSectionAssembler();
    }

    @Bean
    public RecommendationsSectionAssembler recommendationsSectionAssembler() {
        return new RecommendationsSectionAssembler();
    }

    @Bean
    public RisksSectionAssembler risksSectionAssembler() {
        return new RisksSectionAssembler();
    }

    @Bean
    public OpportunitiesSectionAssembler opportunitiesSectionAssembler() {
        return new OpportunitiesSectionAssembler();
    }

    @Bean
    public FinancialSectionAssembler financialSectionAssembler() {
        return new FinancialSectionAssembler();
    }

    @Bean
    public MarketSectionAssembler marketSectionAssembler() {
        return new MarketSectionAssembler();
    }

    @Bean
    public InnovationSectionAssembler innovationSectionAssembler() {
        return new InnovationSectionAssembler();
    }

    @Bean
    public NextStepsSectionAssembler nextStepsSectionAssembler(ReportModel reportModel) {
        return new NextStepsSectionAssembler(reportModel);
    }

    @Bean
    public ReportMetadataAssembler reportMetadataAssembler(ReportModel reportModel) {
        return new ReportMetadataAssembler(reportModel);
    }

    @Bean
    public ReportAssemblers reportAssemblers(
            ExecutiveSummaryAssembler executiveSummary,
            ScoresSectionAssembler scores,
            RecommendationsSectionAssembler recommendations,
            RisksSectionAssembler risks,
            OpportunitiesSectionAssembler opportunities,
            FinancialSectionAssembler financial,
            MarketSectionAssembler market,
            InnovationSectionAssembler innovation,
            NextStepsSectionAssembler nextSteps,
            ReportMetadataAssembler metadata) {
        return new ReportAssemblers(
                executiveSummary,
                scores,
                recommendations,
                risks,
                opportunities,
                financial,
                market,
                innovation,
                nextSteps,
                metadata);
    }

    @Bean
    public ReportEngine reportEngine(ReportAssemblers reportAssemblers, ReportModel reportModel) {
        return new ReportEngine(reportAssemblers, reportModel);
    }

    @Bean
    public ReportStage reportStage(ReportEngine reportEngine) {
        return new ReportStage(reportEngine);
    }

    

    @Bean
    public KnowledgeGateway knowledgeGateway(
            SourceRegistry sourceRegistry,
            SourceValidator sourceValidator,
            org.springframework.beans.factory.ObjectProvider<KnowledgeRepository> knowledgeRepositoryProvider) {
        KnowledgeRepository repository = knowledgeRepositoryProvider.getIfAvailable();
        return new KnowledgeGateway(sourceRegistry, sourceValidator, repository);
    }

    @Bean
    public KnowledgeEngine knowledgeEngine(KnowledgeGateway knowledgeGateway) {
        return new KnowledgeEngine(knowledgeGateway);
    }

    @Bean
    public KnowledgeStage knowledgeStage(
            KnowledgeEngine knowledgeEngine, KinKnowledgeProperties kinKnowledgeProperties) {
        return new KnowledgeStage(knowledgeEngine, kinKnowledgeProperties.isShadowEnabled());
    }

    @Bean
    public EnrichmentStage enrichmentStage(EnrichmentEngine enrichmentEngine) {
        return new EnrichmentStage(enrichmentEngine);
    }

    @Bean
    public ExactMatchStrategy exactMatchStrategy() {
        return new ExactMatchStrategy();
    }

    @Bean
    public FuzzyMatchStrategy fuzzyMatchStrategy() {
        return new FuzzyMatchStrategy(0.85);
    }

    @Bean
    public SemanticMatchStrategy semanticMatchStrategy() {
        return new SemanticMatchStrategy(0.90);
    }

    @Bean
    public DeduplicationEngine deduplicationEngine(List<DeduplicationStrategy> strategies) {
        return new DeduplicationEngine(strategies);
    }

    @Bean
    public DeduplicationStage deduplicationStage(DeduplicationEngine deduplicationEngine) {
        return new DeduplicationStage(deduplicationEngine);
    }

    @Bean
    public AnswerValidator answerValidator() {
        return new AnswerValidator();
    }

    @Bean
    public InterviewBlueprint interviewBlueprint() {
        return new InterviewBlueprint(List.of(
                InterviewQuestion.required("q-proyecto", AnalyzedDimension.PROJECT_NAME, "nombre del proyecto", 1),
                InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR, "sector y giro del negocio", 2),
                InterviewQuestion.required("q-problema", AnalyzedDimension.PROBLEM, "problema que resuelve", 3),
                InterviewQuestion.required("q-solucion", AnalyzedDimension.SOLUTION, "solución propuesta", 4),
                InterviewQuestion.required("q-cliente", AnalyzedDimension.TARGET_CUSTOMER, "cliente objetivo", 5)));
    }

    @Bean
    public InterviewEngine interviewEngine(InterviewBlueprint blueprint, AnswerValidator validator) {
        return new InterviewEngine(blueprint, validator);
    }

    @Bean
    public InterviewRepository interviewRepository(JpaInterviewRepository jpaInterviewRepository) {
        return jpaInterviewRepository;
    }

    @Bean
    public InterviewStage interviewStage(InterviewEngine interviewEngine, InterviewRepository interviewRepository) {
        return new InterviewStage(interviewEngine, interviewRepository);
    }

    @Bean
    public Pipeline chatPipeline(
            AnalyzerStage analyzer,
            TriageStage triage,
            DifferentialStage differential,
            EvaluatorStage evaluator,
            StrategistStage strategist,
            InterviewStage interview,
            KnowledgeStage knowledge,
            DeduplicationStage deduplication,
            EnrichmentStage enrichment,
            ConsultorStage consultor,
            ScoringStage scoring,
            RecommendationStage recommendation,
            RiskStage risk,
            OpportunityStage opportunity,
            ReportStage report,
            EventStage eventStage) {
        return new Pipeline(
                List.of(
                        analyzer,
                        triage,
                        differential,
                        evaluator,
                        strategist,
                        interview,
                        knowledge,
                        deduplication,
                        enrichment,
                        scoring,
                        recommendation,
                        risk,
                        opportunity,
                        report,
                        consultor,
                        eventStage),
                null,
                StageRetryPolicy.none(),
                new StageTimeoutConfig(
                        Map.of(
                                consultor.name(), 60_000L,
                                // Adquisición de conocimiento externo (ADR-021): 6 fuentes
                                // secuenciales con red real; el default de 5 s es insuficiente
                                // (cold start de World Bank/datos.gov.co entre 1 y 20 s por fuente).
                                knowledge.name(), 120_000L),
                        StagePolicy.DEFAULT_TIMEOUT_MILLIS,
                        StageTimeoutConfig.TimeoutAction.FAIL));
    }

    @Bean
    public KinMethod kinMethod(
            Pipeline chatPipeline,
            ContextRepository contextRepository,
            ProjectContextSyncPort projectContextSyncPort,
            EnterprisePipelineResultStore enterprisePipelineResultStore) {
        return new KinMethod(
                chatPipeline,
                contextRepository,
                new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0),
                projectContextSyncPort,
                enterprisePipelineResultStore);
    }

    @Bean
    public ConversationOrchestrator conversationOrchestrator(
            HistoryWindow historyWindow,
            DefaultTurnPolicy turnPolicy,
            KinMethod kinMethod,
            ResponseGuard responseGuard,
            ContextRepository contextRepository,
            EnterpriseProjectTrigger enterpriseProjectTrigger) {
        return new ConversationOrchestrator(
                historyWindow, turnPolicy, kinMethod, responseGuard, contextRepository, enterpriseProjectTrigger);
    }
}













