package com.kinplatform.common.config;

import com.kinplatform.common.ai.interview.adapter.JpaInterviewRepository;
import com.kinplatform.common.ai.knowledge.adapter.KinKnowledgeProperties;
import com.kinplatform.common.ai.provider.AIProvider;
import com.kinplatform.common.ai.provider.ProviderRouter;
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
import com.kinplatform.common.conversation.ResponseFallback;
import com.kinplatform.common.conversation.validation.ResponseGuard;
import com.kinplatform.common.engine.DomainEngine;
import com.kinplatform.common.engine.EngineExecutor;
import com.kinplatform.common.engine.EngineRegistry;
import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.common.interview.InterviewQuestion;
import com.kinplatform.common.interview.InterviewRepository;
import com.kinplatform.common.interview.engine.AnswerValidator;
import com.kinplatform.common.interview.engine.InterviewBlueprint;
import com.kinplatform.common.interview.engine.InterviewEngine;
import com.kinplatform.common.interview.stage.InterviewStage;
import com.kinplatform.common.knowledge.KnowledgeRepository;
import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.deduplication.DeduplicationEngine;
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
import com.kinplatform.common.pipeline.stage.StrategistStage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KinConfig {

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
}