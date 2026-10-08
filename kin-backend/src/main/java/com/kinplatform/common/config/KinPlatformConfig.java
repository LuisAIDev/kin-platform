package com.kinplatform.common.config;

import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.ProjectContextSyncPort;
import com.kinplatform.common.conversation.ConversationOrchestrator;
import com.kinplatform.common.conversation.ResponseFallback;
import com.kinplatform.common.conversation.history.HistoryWindow;
import com.kinplatform.common.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.common.conversation.validation.ResponseGuard;
import com.kinplatform.common.method.KinMethod;
import com.kinplatform.common.pipeline.Pipeline;
import com.kinplatform.common.pipeline.PipelineStage;
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
import com.kinplatform.kin.health.differential.stage.DifferentialStage;
import com.kinplatform.kin.health.triage.stage.TriageStage;
import com.kinplatform.platform.enterprise.application.EnterprisePipelineResultStore;
import com.kinplatform.platform.enterprise.application.EnterpriseProjectTrigger;
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
import com.kinplatform.common.knowledge.deduplication.DeduplicationStage;
import com.kinplatform.common.enrichment.stage.EnrichmentStage;
import com.kinplatform.common.interview.stage.InterviewStage;
import com.kinplatform.common.knowledge.stage.KnowledgeStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "kin.platform", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(KinCommonConfig.class)
public class KinPlatformConfig {

    // ===== Scoring =====
    @Bean
    public ScoringModel scoringModel() {
        return ScoringModel.defaultModel();
    }

    @Bean
    public ScoringEngine scoringEngine(ScoringModel scoringModel) {
        return new ScoringEngine(scoringModel);
    }

    @Bean
    public ScoringStage scoringStage(ScoringEngine scoringEngine) {
        return new ScoringStage(scoringEngine);
    }

    // ===== Recommendation =====
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

    // ===== Risk =====
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

    // ===== Opportunity =====
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
    public OpportunityEngine opportunityEngine(
            List<OpportunityAnalyzer> analyzers, OpportunityModel opportunityModel) {
        return new OpportunityEngine(analyzers, opportunityModel);
    }

    @Bean
    public OpportunityStage opportunityStage(OpportunityEngine opportunityEngine) {
        return new OpportunityStage(opportunityEngine);
    }

    // ===== Report =====
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
                executiveSummary, scores, recommendations, risks, opportunities,
                financial, market, innovation, nextSteps, metadata);
    }

    @Bean
    public ReportEngine reportEngine(ReportAssemblers reportAssemblers, ReportModel reportModel) {
        return new ReportEngine(reportAssemblers, reportModel);
    }

    @Bean
    public ReportStage reportStage(ReportEngine reportEngine) {
        return new ReportStage(reportEngine);
    }

    // ===== Chat Pipeline (compuesto con stages opcionales) =====
    @Bean
    public Pipeline chatPipeline(
            AnalyzerStage analyzer,
            ObjectProvider<TriageStage> triageOpt,
            ObjectProvider<DifferentialStage> differentialOpt,
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
        List<PipelineStage> stages = new ArrayList<>();
        stages.add(analyzer);
        triageOpt.ifAvailable(stages::add);
        differentialOpt.ifAvailable(stages::add);
        stages.add(evaluator);
        stages.add(strategist);
        stages.add(interview);
        stages.add(knowledge);
        stages.add(deduplication);
        stages.add(enrichment);
        stages.add(scoring);
        stages.add(recommendation);
        stages.add(risk);
        stages.add(opportunity);
        stages.add(report);
        stages.add(consultor);
        stages.add(eventStage);

        return new Pipeline(
                stages,
                null,
                StageRetryPolicy.none(),
                new StageTimeoutConfig(
                        Map.of(
                                consultor.name(), 60_000L,
                                knowledge.name(), 120_000L),
                        StagePolicy.DEFAULT_TIMEOUT_MILLIS,
                        StageTimeoutConfig.TimeoutAction.FAIL));
    }

    // ===== Bridge beans =====
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