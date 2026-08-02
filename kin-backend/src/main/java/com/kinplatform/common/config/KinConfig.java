package com.kinplatform.common.config;

import com.kinplatform.ai.interview.adapter.JpaInterviewRepository;
import com.kinplatform.ai.provider.AIProvider;
import com.kinplatform.ai.provider.DeepSeekProvider;
import com.kinplatform.ai.provider.OpenAIProvider;
import com.kinplatform.ai.provider.ProviderRouter;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;
import com.kinplatform.kin.ai.prompt.SectionFormatter;
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
import com.kinplatform.kin.ai.prompt.formatter.SourcesSectionFormatter;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.ContextAnalyzerPort;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineExecutor;
import com.kinplatform.kin.engine.EngineRegistry;
import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.enrichment.FactRanker;
import com.kinplatform.kin.enrichment.stage.EnrichmentStage;
import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.knowledge.stage.KnowledgeStage;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewRepository;
import com.kinplatform.kin.interview.engine.AnswerValidator;
import com.kinplatform.kin.interview.engine.InterviewBlueprint;
import com.kinplatform.kin.interview.engine.InterviewEngine;
import com.kinplatform.kin.interview.stage.InterviewStage;
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
import com.kinplatform.kin.reporting.risk.BusinessRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.FinancialRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.MarketRiskAnalyzer;
import com.kinplatform.kin.reporting.risk.RiskAnalyzer;
import com.kinplatform.kin.reporting.risk.RiskEngine;
import com.kinplatform.kin.reporting.risk.RiskModel;
import com.kinplatform.kin.reporting.risk.TechnicalRiskAnalyzer;
import com.kinplatform.kin.reporting.opportunity.AutomationOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.CompetitiveOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.FinancialOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.InnovationOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.MarketOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.MonetizationOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.OpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.OpportunityEngine;
import com.kinplatform.kin.reporting.opportunity.OpportunityModel;
import com.kinplatform.kin.reporting.opportunity.ScalabilityOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.TechnologicalOpportunityAnalyzer;
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
import com.kinplatform.kin.scoring.ScoringEngine;
import com.kinplatform.kin.scoring.ScoringModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class KinConfig {

    @Bean
    public EvaluationPolicies evaluationPolicies() {
        return EvaluationPolicies.defaults();
    }

    @Bean
    public ExplorationPriority explorationPriority() {
        return ExplorationPriority.defaultPriorities();
    }

    @Bean
    public DefaultExplorationStrategy defaultExplorationStrategy(ExplorationPriority priority) {
        return new DefaultExplorationStrategy(priority);
    }

    @Bean
    public ConversationStrategist conversationStrategist(DefaultExplorationStrategy defaultStrategy) {
        return new ConversationStrategist(defaultStrategy);
    }

    @Bean
    public CompletenessEvaluator completenessEvaluator(EvaluationPolicies policies) {
        return new CompletenessEvaluator(policies);
    }

    @Bean
    public DomainEventBus domainEventBus() {
        return new InMemoryDomainEventBus();
    }

    @Bean
    public ScoringModel scoringModel() {
        return ScoringModel.defaultModel();
    }

    @Bean
    public ScoringEngine scoringEngine(ScoringModel scoringModel) {
        return new ScoringEngine(scoringModel);
    }

    @Bean
    public ProviderRouter providerRouter(List<AIProvider> aiProviders) {
        return new ProviderRouter(aiProviders);
    }

    @Bean
    public ConversationPromptBuilder conversationPromptBuilder() {
        return new ConversationPromptBuilder();
    }

    @Bean
    public ReportPromptBuilder reportPromptBuilder(List<SectionFormatter<?>> sectionFormatters) {
        return new ReportPromptBuilder(sectionFormatters);
    }

    @Bean
    public PromptAssembler promptAssembler(ConversationPromptBuilder conversationBuilder,
                                           ReportPromptBuilder reportBuilder) {
        return new PromptAssembler(conversationBuilder, reportBuilder);
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
    public ConsultorStage consultorStage(AIResponder aiResponder, PromptAssembler promptAssembler,
                                         ResponseGuard responseGuard) {
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
        return new ReportAssemblers(executiveSummary, scores, recommendations, risks,
            opportunities, financial, market, innovation, nextSteps, metadata);
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
    public EventStage eventStage() {
        return new EventStage();
    }

    @Bean
    public EngineExecutor engineExecutor() {
        return new EngineExecutor();
    }

    @Bean
    public EngineRegistry engineRegistry(List<DomainEngine<?, ?>> domainEngines) {
        return new EngineRegistry(domainEngines);
    }

    @Bean
    public SourceValidator sourceValidator() {
        return SourceValidator.strict();
    }

    @Bean
    public SourceRegistry sourceRegistry(List<KnowledgeSource> knowledgeSources) {
        return new SourceRegistry(knowledgeSources);
    }

    @Bean
    public KnowledgeGateway knowledgeGateway(SourceRegistry sourceRegistry, SourceValidator sourceValidator) {
        return new KnowledgeGateway(sourceRegistry, sourceValidator);
    }

    @Bean
    public KnowledgeEngine knowledgeEngine(KnowledgeGateway knowledgeGateway) {
        return new KnowledgeEngine(knowledgeGateway);
    }

    @Bean
    public KnowledgeStage knowledgeStage(KnowledgeEngine knowledgeEngine) {
        return new KnowledgeStage(knowledgeEngine);
    }

    @Bean
    public FactRanker factRanker() {
        return new FactRanker();
    }

    @Bean
    public EnrichmentEngine enrichmentEngine(FactRanker factRanker) {
        return new EnrichmentEngine(factRanker);
    }

    @Bean
    public EnrichmentStage enrichmentStage(EnrichmentEngine enrichmentEngine) {
        return new EnrichmentStage(enrichmentEngine);
    }

    @Bean
    public AnswerValidator answerValidator() {
        return new AnswerValidator();
    }

    @Bean
    public InterviewBlueprint interviewBlueprint() {
        return new InterviewBlueprint(List.of(
            InterviewQuestion.required("q-proyecto", AnalyzedDimension.PROJECT_NAME,
                "nombre del proyecto", 1),
            InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR,
                "sector y giro del negocio", 2),
            InterviewQuestion.required("q-problema", AnalyzedDimension.PROBLEM,
                "problema que resuelve", 3),
            InterviewQuestion.required("q-solucion", AnalyzedDimension.SOLUTION,
                "solución propuesta", 4),
            InterviewQuestion.required("q-cliente", AnalyzedDimension.TARGET_CUSTOMER,
                "cliente objetivo", 5)));
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
            EvaluatorStage evaluator,
            StrategistStage strategist,
            InterviewStage interview,
            KnowledgeStage knowledge,
            EnrichmentStage enrichment,
            ConsultorStage consultor,
            ScoringStage scoring,
            RecommendationStage recommendation,
            RiskStage risk,
            OpportunityStage opportunity,
            ReportStage report,
            EventStage eventStage) {
        return new Pipeline(List.of(analyzer, evaluator, strategist, interview, knowledge,
            enrichment, scoring, recommendation, risk, opportunity, report, consultor, eventStage));
    }

    @Bean
    public KinMethod kinMethod(Pipeline chatPipeline, DomainEventBus eventBus, ContextRepository contextRepository) {
        return new KinMethod(chatPipeline, eventBus, contextRepository);
    }

    @Bean
    public DefaultTurnPolicy defaultTurnPolicy() {
        return new DefaultTurnPolicy();
    }

    @Bean
    public ResponseGuard responseGuard() {
        return new ResponseGuard();
    }

    @Bean
    public HistoryWindow historyWindow() {
        return new HistoryWindow();
    }

    @Bean
    public ConversationOrchestrator conversationOrchestrator(HistoryWindow historyWindow,
                                                             DefaultTurnPolicy turnPolicy,
                                                             KinMethod kinMethod,
                                                             ResponseGuard responseGuard,
                                                             ContextRepository contextRepository) {
        return new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod,
            responseGuard, contextRepository);
    }
}
