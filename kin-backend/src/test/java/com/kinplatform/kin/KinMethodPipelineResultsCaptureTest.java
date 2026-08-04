package com.kinplatform.kin;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.ResponseFallback;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enterprise.application.EnterprisePipelineResultStore;
import com.kinplatform.kin.enterprise.application.EnterpriseTurnResults;
import com.kinplatform.kin.enterprise.application.InMemoryEnterprisePipelineResultStore;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de captura de los resultados reales del pipeline en el runtime (Fase
 * 10, Milestone 3C): {@link KinMethod} publica los resultados del turno
 * {@code REPORT} en la {@link EnterprisePipelineResultStore}, reutilizando
 * exactamente lo que produjo el pipeline (sin recalcular ni re-ejecutar
 * motores).
 */
class KinMethodPipelineResultsCaptureTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void execute_conReporte_publicaLosResultadosRealesEnElStore() {
        var store = new InMemoryEnterprisePipelineResultStore();
        var kinMethod = kinMethodConStore(pipelineReporteConResultados(), store);

        kinMethod.execute(command("generá el informe"));

        var results = store.consume(PROJECT_ID);
        assertTrue(results.isPresent());
        assertFalse(results.get().recommendations().isEmpty());
        assertFalse(results.get().opportunities().isEmpty());
        assertFalse(results.get().knowledge().isEmpty());
        assertFalse(results.get().riskResult().isEmpty());
    }

    @Test
    void execute_conTurnoAsk_noPublicaResultados() {
        var store = new InMemoryEnterprisePipelineResultStore();
        var kinMethod = kinMethodConStore(pipelineAsk(), store);

        kinMethod.execute(command("contame tu problema"));

        assertTrue(store.consume(PROJECT_ID).isEmpty());
    }

    @Test
    void execute_conReporte_sinInforme_noPublicaResultados() {
        var store = new InMemoryEnterprisePipelineResultStore();
        var kinMethod = kinMethodConStore(pipelineReporteSinInforme(), store);

        kinMethod.execute(command("generá el informe"));

        assertTrue(store.consume(PROJECT_ID).isEmpty());
    }

    @Test
    void execute_sinStore_conservaElComportamientoPrevio() {
        var kinMethod = kinMethodSinStore(pipelineReporteConResultados());

        var result = kinMethod.execute(command("generá el informe"));

        assertTrue(result.decision().action() == ConversationDecision.Action.REPORT);
    }

    private KinMethod kinMethodConStore(Pipeline pipeline,
                                        EnterprisePipelineResultStore store) {
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(contextoCompleto());
        return new KinMethod(pipeline, new InMemoryDomainEventBus(), contextRepository,
            new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0),
            (projectId, context) -> { }, store);
    }

    private KinMethod kinMethodSinStore(Pipeline pipeline) {
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(contextoCompleto());
        return new KinMethod(pipeline, new InMemoryDomainEventBus(), contextRepository);
    }

    private Pipeline pipelineReporteConResultados() {
        return new Pipeline(List.of((PipelineStage) new PipelineStage() {
            @Override
            public String name() {
                return "ReporteTest";
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

    private Pipeline pipelineReporteSinInforme() {
        return new Pipeline(List.of((PipelineStage) new PipelineStage() {
            @Override
            public String name() {
                return "ReporteSinInforme";
            }

            @Override
            public boolean supports(PipelineContext context) {
                return true;
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                context.projectContext(contextoCompleto());
                context.decision(ConversationDecision.generateReport("informe"));
                context.aiResponse("Informe generado.");
                context.markCompleted();
                return context;
            }
        }));
    }

    private Pipeline pipelineAsk() {
        return new Pipeline(List.of((PipelineStage) new PipelineStage() {
            @Override
            public String name() {
                return "AskTest";
            }

            @Override
            public boolean supports(PipelineContext context) {
                return true;
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                context.projectContext(contextoCompleto());
                context.decision(ConversationDecision.ask(
                    AnalyzedDimension.PROBLEM, 10, "¿Cuál es el problema?"));
                context.aiResponse("¿Cuál es el problema que resolvés?");
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

    private KinMethodCommand command(String message) {
        return new KinMethodCommand(
            PROJECT_ID, USER_ID, message, List.of(),
            "Proyecto Test", "Descripción", "Software");
    }
}
