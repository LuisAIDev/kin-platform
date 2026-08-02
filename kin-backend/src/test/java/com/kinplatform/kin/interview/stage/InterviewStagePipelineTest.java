package com.kinplatform.kin.interview.stage;

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
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewRepository;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.interview.InMemoryInterviewRepository;
import com.kinplatform.kin.interview.engine.AnswerValidator;
import com.kinplatform.kin.interview.engine.InterviewBlueprint;
import com.kinplatform.kin.interview.engine.InterviewEngine;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewStagePipelineTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AIResponder aiResponder;

    private InterviewStage interviewStage() {
        return interviewStage(new InMemoryInterviewRepository());
    }

    private InterviewStage interviewStage(InterviewRepository repository) {
        var blueprint = new InterviewBlueprint(List.of(
            InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR, "sector del negocio", 1),
            InterviewQuestion.required("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", 2)));
        return new InterviewStage(new InterviewEngine(blueprint, new AnswerValidator()), repository);
    }

    private InterviewState interviewCompleta() {
        return InterviewState.empty(PROJECT_ID)
            .withAnswered(Map.of(
                "q-sector", InterviewAnswer.of("q-sector", "Restaurante"),
                "q-revenue", InterviewAnswer.of("q-revenue", "Ventas directas")))
            .withPending(List.of())
            .withComplete(true);
    }

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

    private List<PipelineStage> officialStages(InterviewStage interview) {
        return List.of(
            new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            interview,
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
            new EventStage());
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
    void pipeline_deberiaEjecutarInterviewStage_conProjectContext() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = new Pipeline(officialStages(interviewStage())).execute(context(fullContext()));

        assertNotNull(result.interviewResult());
        assertTrue(result.interviewResult().hasDirective());
        assertEquals(InterviewEngine.GENERATOR_NAME, result.interviewResult().generatedBy());
        assertTrue(result.engineResults().containsKey(InterviewEngine.GENERATOR_NAME));
        assertNotNull(result.knowledgeResult());
    }

    @Test
    void pipeline_deberiaOmitirInterviewStage_cuandoNoHayProjectContext() {
        var pipeline = new Pipeline(List.of(interviewStage()));
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "hola", List.of(),
            "T", "D", "C");

        var result = pipeline.execute(ctx);

        assertNull(result.interviewResult());
    }

    @Test
    void pipeline_deberiaEjecutarInterviewStageEnElOrdenOficial_conEntrevistaCompleta() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var order = new ArrayList<String>();
        var pipeline = new Pipeline(officialStages(
            interviewStage(new InMemoryInterviewRepository(interviewCompleta()))).stream()
            .map(s -> record(s, order))
            .toList());

        pipeline.execute(context(fullContext()));

        assertEquals(List.of("Analizador", "Evaluador", "Estratega", "Entrevista",
            "Conocimiento", "Scoring", "Recomendaciones", "Riesgos", "Oportunidades",
            "Reporte", "Consultor", "Eventos"), order);
    }

    @Test
    void pipeline_deberiaDejarInterviewResultDisponibleParaElConsultor() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = new Pipeline(officialStages(interviewStage())).execute(context(fullContext()));

        assertNotNull(result.interviewResult());
        assertNotNull(result.aiResponse());
        assertFalse(result.aiResponse().isBlank());
    }

    @Test
    void pipeline_deberiaCompletarElPipelineConInterviewStageIntegrado_conEntrevistaCompleta() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("=== INFORME DE VIABILIDAD ===");

        var result = new Pipeline(officialStages(
            interviewStage(new InMemoryInterviewRepository(interviewCompleta()))))
            .execute(context(fullContext()));

        assertNotNull(result.scoreResult());
        assertNotNull(result.recommendationResult());
        assertNotNull(result.riskResult());
        assertNotNull(result.opportunityResult());
        assertNotNull(result.consultingReport());
        assertNotNull(result.knowledgeResult());
        assertNotNull(result.interviewResult());
        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertEquals(10, result.consultingReport().metadata().sectionsIncluded().size());
        assertTrue(result.completed());
        assertFalse(result.events().isEmpty());
    }

    @Test
    void pipeline_deberiaBloquearElReporte_cuandoLaEntrevistaEstaIncompleta() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿En qué rubro vas a operar?");

        var result = new Pipeline(officialStages(interviewStage())).execute(context(fullContext()));

        assertEquals(ConversationDecision.Action.ASK, result.decision().action());
        assertNull(result.scoreResult());
        assertNull(result.consultingReport());
        assertNotNull(result.interviewResult());
        assertTrue(result.interviewResult().hasDirective());
    }

    @Test
    void pipeline_deberiaPersistirElEstadoEntreTurnos() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿Pregunta?");

        var repo = new InMemoryInterviewRepository();
        var pipeline = new Pipeline(officialStages(interviewStage(repo)));

        var primerTurno = pipeline.execute(context(fullContext()));
        var segundoTurno = pipeline.execute(context(fullContext()));

        assertNotNull(primerTurno.interviewResult());
        assertNotNull(segundoTurno.interviewResult());
        var persisted = repo.find(PROJECT_ID).orElseThrow();
        assertTrue(persisted.hasAnswered("q-sector"));
        assertEquals("q-revenue", persisted.current());
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
