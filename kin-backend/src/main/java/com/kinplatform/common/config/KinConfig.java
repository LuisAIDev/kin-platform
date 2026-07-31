package com.kinplatform.common.config;

import com.kinplatform.ai.provider.AIProvider;
import com.kinplatform.ai.provider.DeepSeekProvider;
import com.kinplatform.ai.provider.OpenAIProvider;
import com.kinplatform.ai.provider.ProviderRouter;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.ContextAnalyzerPort;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineExecutor;
import com.kinplatform.kin.engine.EngineRegistry;
import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.stage.AnalyzerStage;
import com.kinplatform.kin.pipeline.stage.ConsultorStage;
import com.kinplatform.kin.pipeline.stage.EvaluatorStage;
import com.kinplatform.kin.pipeline.stage.EventStage;
import com.kinplatform.kin.pipeline.stage.OpportunityStage;
import com.kinplatform.kin.pipeline.stage.RecommendationStage;
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
    public PromptAssembler promptAssembler() {
        return new PromptAssembler();
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
    public ConsultorStage consultorStage(AIResponder aiResponder, PromptAssembler promptAssembler) {
        return new ConsultorStage(aiResponder, promptAssembler);
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
    public Pipeline chatPipeline(
            AnalyzerStage analyzer,
            EvaluatorStage evaluator,
            StrategistStage strategist,
            ConsultorStage consultor,
            ScoringStage scoring,
            RecommendationStage recommendation,
            RiskStage risk,
            OpportunityStage opportunity,
            EventStage eventStage) {
        return new Pipeline(List.of(analyzer, evaluator, strategist, consultor,
            scoring, recommendation, risk, opportunity, eventStage));
    }

    @Bean
    public KinMethod kinMethod(Pipeline chatPipeline, DomainEventBus eventBus, ContextRepository contextRepository) {
        return new KinMethod(chatPipeline, eventBus, contextRepository);
    }
}
