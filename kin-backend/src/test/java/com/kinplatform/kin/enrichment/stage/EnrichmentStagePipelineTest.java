package com.kinplatform.kin.enrichment.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.FactRanker;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
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
import com.kinplatform.kin.pipeline.stage.EvaluatorStage;
import com.kinplatform.kin.pipeline.stage.EventStage;
import com.kinplatform.kin.pipeline.stage.OpportunityStage;
import com.kinplatform.kin.pipeline.stage.RecommendationStage;
import com.kinplatform.kin.pipeline.stage.ReportStage;
import com.kinplatform.kin.pipeline.stage.RiskStage;
import com.kinplatform.kin.pipeline.stage.ScoringStage;
import com.kinplatform.kin.pipeline.stage.StrategistStage;
import com.kinplatform.kin.reporting.RecommendationEngine;
import com.kinplatform.kin.reporting.RecommendationInput;
import com.kinplatform.kin.reporting.RecommendationModel;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.MarketOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.MonetizationOpportunityAnalyzer;
import com.kinplatform.kin.reporting.opportunity.OpportunityEngine;
import com.kinplatform.kin.reporting.opportunity.OpportunityInput;
import com.kinplatform.kin.reporting.opportunity.OpportunityModel;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
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
import com.kinplatform.kin.reporting.risk.RiskInput;
import com.kinplatform.kin.reporting.risk.RiskModel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoringEngine;
import com.kinplatform.kin.scoring.ScoringModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnrichmentStagePipelineTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private KnowledgeStage knowledgeStage() {
        var gateway = new KnowledgeGateway(SourceRegistry.empty(), SourceValidator.strict());
        return new KnowledgeStage(new KnowledgeEngine(gateway));
    }

    private KnowledgeStage knowledgeStageWithData() {
        var candidate = new KnowledgeCandidate(
            "El mercado retail crece con demanda del consumidor. Dato verificado.",
            "src-1", "Fuente", "https://example.com/report",
            OffsetDateTime.now().minusDays(10), "application/json",
            Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
        var source = new CapturingSource(candidate);
        var validator = new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json", "text/plain"));
        return new KnowledgeStage(new KnowledgeEngine(
            new KnowledgeGateway(new SourceRegistry(List.of(source)), validator)));
    }

    private EnrichmentStage enrichmentStage() {
        return new EnrichmentStage(new EnrichmentEngine(new FactRanker()));
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

    private List<PipelineStage> analysisStages(KnowledgeStage knowledge) {
        return List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            knowledge,
            enrichmentStage(),
            new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
            new RecommendationStage(new RecommendationEngine(RecommendationModel.defaultModel())),
            new RiskStage(new RiskEngine(
                List.of(new BusinessRiskAnalyzer(), new MarketRiskAnalyzer()),
                RiskModel.defaultModel())),
            new OpportunityStage(new OpportunityEngine(
                List.of(new MarketOpportunityAnalyzer(), new MonetizationOpportunityAnalyzer()),
                OpportunityModel.defaultModel())),
            new ReportStage(reportEngine()),
            new EventStage()
        );
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
    void pipeline_deberiaEjecutarEnrichmentStage_conProjectContext() {
        var result = new Pipeline(analysisStages(knowledgeStage())).execute(context(fullContext()));

        assertNotNull(result.enrichmentResult());
        assertTrue(result.enrichmentResult().isEmpty());
        assertEquals(0, result.enrichmentResult().totalEvidence());
        assertTrue(result.engineResults().containsKey(EnrichmentEngine.GENERATOR_NAME));
    }

    @Test
    void pipeline_deberiaOmitirEnrichmentStage_cuandoNoHayProjectContext() {
        var pipeline = new Pipeline(List.of(knowledgeStage(), enrichmentStage()));
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "hola", List.of(), "T", "D", "C");

        var result = pipeline.execute(ctx);

        assertNull(result.enrichmentResult());
    }

    @Test
    void pipeline_deberiaEnriquecerElAnalisis_cuandoHayConocimientoRelevante() {
        var result = new Pipeline(analysisStages(knowledgeStageWithData()))
            .execute(context(fullContext()));

        EnrichmentResult enrichment = result.enrichmentResult();
        assertNotNull(enrichment);
        assertFalse(enrichment.isEmpty());
        assertTrue(enrichment.totalEvidence() >= 1);
        assertEquals(EnrichmentEngine.GENERATOR_NAME, enrichment.generatedBy());
        assertNotNull(result.consultingReport());
        assertFalse(result.consultingReport().sources().isEmpty());
        assertEquals(1, result.consultingReport().sources().sources().size());
        assertEquals("src-1", result.consultingReport().sources().sources().get(0).sourceId());
        assertTrue(result.consultingReport().metadata().sectionsIncluded().contains("Sources"));
    }

    @Test
    void pipeline_deberiaEjecutarEnrichmentStageEnElOrdenOficial() {
        var order = new ArrayList<String>();
        var stages = analysisStages(knowledgeStageWithData()).stream()
            .map(s -> record(s, order))
            .toList();

        new Pipeline(stages).execute(context(fullContext()));

        assertEquals(List.of("Analizador", "Evaluador", "Estratega", "Conocimiento",
            "Enriquecimiento", "Scoring", "Recomendaciones", "Riesgos", "Oportunidades",
            "Reporte", "Eventos"), order);
    }

    @Test
    void pipeline_deberiaDejarEnrichmentResultDisponibleParaReportEngine() {
        var result = new Pipeline(analysisStages(knowledgeStageWithData()))
            .execute(context(fullContext()));

        assertNotNull(result.knowledgeResult());
        assertFalse(result.knowledgeResult().isEmpty());
        assertNotNull(result.consultingReport());
        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertFalse(result.consultingReport().sources().isEmpty());
        assertNotNull(result.engineResult(EnrichmentEngine.GENERATOR_NAME));
    }

    @Test
    void pipeline_deberiaCompletarElPipelineConEnrichmentStageIntegrado_sinConocimiento() {
        var result = new Pipeline(analysisStages(knowledgeStage()))
            .execute(context(fullContext()));

        assertNotNull(result.scoreResult());
        assertNotNull(result.recommendationResult());
        assertNotNull(result.riskResult());
        assertNotNull(result.opportunityResult());
        assertNotNull(result.consultingReport());
        assertNotNull(result.knowledgeResult());
        assertNotNull(result.enrichmentResult());
        assertTrue(result.enrichmentResult().isEmpty());
        assertEquals(10, result.consultingReport().metadata().sectionsIncluded().size());
        assertTrue(result.completed());
        assertFalse(result.events().isEmpty());
    }

    @Test
    void pipeline_deberiaPasarElEnrichmentResultALasEtapasDeAnalisis_cuandoExiste() {
        var recEngine = mock(RecommendationEngine.class);
        var riskEngine = mock(RiskEngine.class);
        var oppEngine = mock(OpportunityEngine.class);
        var recCaptured = new AtomicReference<EnrichmentResult>();
        var riskCaptured = new AtomicReference<EnrichmentResult>();
        var oppCaptured = new AtomicReference<EnrichmentResult>();

        when(recEngine.metadata()).thenReturn(EngineMetadata.of("RecommendationEngine", "v1",
            "KIN", EnginePhase.RECOMMENDATION, EngineType.DOMAIN, 40));
        when(riskEngine.metadata()).thenReturn(EngineMetadata.of("RiskEngine", "v1",
            "KIN", EnginePhase.RISK, EngineType.DOMAIN, 50));
        when(oppEngine.metadata()).thenReturn(EngineMetadata.of("OpportunityEngine", "v1",
            "KIN", EnginePhase.OPPORTUNITY, EngineType.DOMAIN, 60));
        when(recEngine.evaluate(any(RecommendationInput.class)))
            .thenAnswer(inv -> { recCaptured.set(inv.<RecommendationInput>getArgument(0).enrichment());
                return RecommendationResult.empty(); });
        when(riskEngine.evaluate(any(RiskInput.class)))
            .thenAnswer(inv -> { riskCaptured.set(inv.<RiskInput>getArgument(0).enrichment());
                return RiskResult.empty(); });
        when(oppEngine.evaluate(any(OpportunityInput.class)))
            .thenAnswer(inv -> { oppCaptured.set(inv.<OpportunityInput>getArgument(0).enrichment());
                return OpportunityResult.empty(); });

        var pipeline = new Pipeline(List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            knowledgeStageWithData(),
            enrichmentStage(),
            new ScoringStage(new ScoringEngine(ScoringModel.defaultModel())),
            new RecommendationStage(recEngine),
            new RiskStage(riskEngine),
            new OpportunityStage(oppEngine)
        ));

        var result = pipeline.execute(context(fullContext()));

        EnrichmentResult expected = result.enrichmentResult();
        assertNotNull(expected);
        assertFalse(expected.isEmpty());
        assertEquals(expected, recCaptured.get());
        assertEquals(expected, riskCaptured.get());
        assertEquals(expected, oppCaptured.get());
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

    private static class CapturingSource implements KnowledgeSource {
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
