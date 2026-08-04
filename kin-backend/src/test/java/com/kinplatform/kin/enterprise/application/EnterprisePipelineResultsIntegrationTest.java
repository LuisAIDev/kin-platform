package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.KinMethodCommand;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.engine.DefaultBusinessModelEngine;
import com.kinplatform.kin.enterprise.engine.DefaultEnterpriseScoreEngine;
import com.kinplatform.kin.enterprise.engine.DefaultFinancialPlanEngine;
import com.kinplatform.kin.enterprise.engine.DefaultInnovationEngine;
import com.kinplatform.kin.enterprise.engine.DefaultKpiEngine;
import com.kinplatform.kin.enterprise.engine.DefaultMarketEngine;
import com.kinplatform.kin.enterprise.engine.DefaultRiskPlanEngine;
import com.kinplatform.kin.enterprise.engine.DefaultRoadmapEngine;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba de integración de extremo a extremo de M3C (Fase 10): el pipeline real
 * (runtime) produce los resultados del turno {@code REPORT}, el runtime los
 * publica en el {@link EnterprisePipelineResultStore}, el ciclo automático
 * (trigger → evento → listener) los fusiona en la
 * {@code EnterpriseGenerationRequest} y la generación produce documentos con
 * datos reales (TAM/SAM/SOM, riesgos, oportunidades, recomendaciones,
 * conocimiento) en lugar de placeholders.
 */
class EnterprisePipelineResultsIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void pipelineReport_generaProyectoEnterpriseConDatosReales() {
        var enterpriseRepository = new InMemoryEnterpriseProjectRepository();
        var bus = new InMemoryDomainEventBus();
        var store = new InMemoryEnterprisePipelineResultStore();
        var contextRepository = mock(ContextRepository.class);
        var contexto = contextoCompleto();
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(contexto);
        when(contextRepository.find(PROJECT_ID)).thenReturn(java.util.Optional.of(contexto));

        var kinMethod = new KinMethod(pipelineReporteConResultados(), bus, contextRepository,
            new com.kinplatform.kin.conversation.ResponseFallback(
                List.of(com.kinplatform.kin.conversation.ResponseFallback.DEFAULT_CANNED_RESPONSE), 0),
            (projectId, ctx) -> { }, store);
        var trigger = new DefaultEnterpriseProjectTrigger(enterpriseRepository, bus);
        var orchestrator = new ConversationOrchestrator(new HistoryWindow(),
            new DefaultTurnPolicy(), kinMethod, new ResponseGuard(), contextRepository, trigger);
        var service = new EnterpriseGenerationService(
            new DefaultBusinessModelEngine(), new DefaultMarketEngine(),
            new DefaultInnovationEngine(), new DefaultFinancialPlanEngine(),
            new DefaultRoadmapEngine(), new DefaultRiskPlanEngine(),
            new DefaultKpiEngine(), new DefaultEnterpriseScoreEngine(),
            new EnterpriseDocumentAssembler(), enterpriseRepository, bus, Runnable::run);
        var generationOrchestrator = new EnterpriseGenerationOrchestrator(service);
        new EnterpriseProjectRequestedListener(generationOrchestrator, contextRepository, bus,
            Runnable::run, store);

        var result = orchestrator.orchestrate(new ConversationTurn(
            PROJECT_ID, USER_ID, "Generá el informe de viabilidad", List.of(),
            "Proyecto Test", "Descripción", "Software"));

        assertNotNull(result.consultingReport());
        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());

        EnterpriseProject generated = enterpriseRepository.findLatestVersion(PROJECT_ID).orElseThrow();
        assertTrue(generated.isCompleted());
        assertEquals(7, generated.documentCount());
        assertNotNull(generated.score(), "El Enterprise Score debe persistirse en el aggregate completado");
        assertTrue(generated.score().overallScore() > 0, "El Enterprise Score debe calcularse sobre datos reales");

        String market = documento(generated, DocumentType.MARKET_PLAN);
        assertTrue(market.contains("1000000"), "TAM real del KnowledgeResult no llegó al plan de mercado: " + market);

        String risks = documento(generated, DocumentType.RISK_MATRIX);
        assertTrue(risks.contains("0.75"),
            "Riesgo real HIGH del RiskResult no llegó a la matriz (probabilidad 0.75): " + risks);

        String innovation = documento(generated, DocumentType.INNOVATION_PLAN);
        assertTrue(innovation.contains("Innovación de proceso"),
            "Oportunidad real del OpportunityResult no llegó al plan de innovación: " + innovation);

        String roadmap = documento(generated, DocumentType.ROADMAP);
        assertTrue(roadmap.contains("validation"),
            "Recomendación real del RecommendationResult (fase VALIDATION) no llegó a la hoja de ruta: " + roadmap);
    }

    private String documento(EnterpriseProject project, DocumentType type) {
        return project.documents().stream()
            .filter(d -> d.type() == type)
            .findFirst()
            .map(d -> d.content())
            .orElse("");
    }

    private Pipeline pipelineReporteConResultados() {
        return new Pipeline(List.of((PipelineStage) new PipelineStage() {
            @Override
            public String name() {
                return "ReporteReal";
            }

            @Override
            public boolean supports(PipelineContext context) {
                return true;
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                context.projectContext(contextoCompleto());
                context.decision(ConversationDecision.generateReport("informe"));
                context.consultingReport(ConsultingReport.empty());
                context.recommendationResult(EngineTestFixtures.recommendations(0.8));
                context.opportunityResult(EngineTestFixtures.opportunities(0.8));
                context.knowledgeResult(EngineTestFixtures.knowledge(0.8));
                context.riskResult(EngineTestFixtures.riskResult(0.8));
                context.aiResponse("Informe de viabilidad completo del proyecto.");
                context.markCompleted();
                return context;
            }
        }));
    }

    private ProjectContext contextoCompleto() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        return ProjectContext.restore(
            data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false);
    }
}
