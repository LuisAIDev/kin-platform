package com.kinplatform.kin.conversation;

import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.KinMethodCommand;
import com.kinplatform.kin.KinMethodResult;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.policy.TurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enterprise.application.DefaultEnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationService;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectRequestedListener;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.application.InMemoryEnterpriseProjectRepository;
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
import com.kinplatform.kin.enterprise.events.EnterpriseProjectGenerated;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la integración Enterprise (Fase 10, Milestone 2F): punto mínimo de
 * emisión en el {@link ConversationOrchestrator} y cadena completa
 * {@code pipeline REPORT → EnterpriseProjectRequested → listener →
 * generación asíncrona}.
 */
@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorEnterpriseTriggerTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private KinMethod kinMethod;

    @Mock
    private ContextRepository contextRepository;

    private final HistoryWindow historyWindow = new HistoryWindow();
    private final TurnPolicy turnPolicy = new DefaultTurnPolicy();
    private final ResponseGuard responseGuard = new ResponseGuard();

    // ------------------------------------------------------------------
    // Emisión en el orquestador
    // ------------------------------------------------------------------

    private ConversationOrchestrator orchestrator(EnterpriseProjectTrigger trigger) {
        return new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod,
            responseGuard, contextRepository, trigger);
    }

    private ProjectContext contextoReporte() {
        var contexto = ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
        contexto.markReportGenerated();
        contexto.attachDecision(ConversationDecision.generateReport("informe"));
        return contexto;
    }

    private ConversationTurn turn() {
        return new ConversationTurn(PROJECT_ID, USER_ID, "Generá el informe", List.of(),
            "Proyecto Test", "Descripción", "Software");
    }

    private void stubContexto(ProjectContext ctx) {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(ctx);
    }

    private void stubReporte(ConsultingReport reporte) {
        var contexto = contextoReporte();
        stubContexto(contexto);
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(new KinMethodResult(
            contexto, null, ConversationDecision.generateReport("informe"),
            "Aquí tenés el informe de viabilidad completo.", null, List.of(), reporte));
    }

    @Test
    void turnoReport_conInforme_llamaAlTrigger() {
        var trigger = mock(EnterpriseProjectTrigger.class);
        stubReporte(ConsultingReport.empty());

        var result = orchestrator(trigger).orchestrate(turn());

        assertNotNull(result.consultingReport());
        assertTrue(result.validation().accepted());
        verify(trigger).request(PROJECT_ID);
    }

    @Test
    void turnoReport_sinInforme_noLlamaAlTrigger() {
        var trigger = mock(EnterpriseProjectTrigger.class);
        stubReporte(null);

        orchestrator(trigger).orchestrate(turn());

        verify(trigger, never()).request(any());
    }

    @Test
    void turnoAsk_noLlamaAlTrigger() {
        var trigger = mock(EnterpriseProjectTrigger.class);
        var contexto = ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
        stubContexto(contexto);
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(new KinMethodResult(
            contexto, null,
            ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta"),
            "¿Apuntás a consumidores o a empresas?", null, List.of(), null));

        orchestrator(trigger).orchestrate(turn());

        verify(trigger, never()).request(any());
    }

    @Test
    void turnoReport_sinTrigger_integraNoOpYElTurnoCompletaIgual() {
        stubReporte(ConsultingReport.empty());

        var result = new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod,
            responseGuard, contextRepository).orchestrate(turn());

        assertNotNull(result.consultingReport());
        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertTrue(result.validation().accepted());
    }

    // ------------------------------------------------------------------
    // Cadena completa pipeline → generación Enterprise
    // ------------------------------------------------------------------

    private void stubCadenaCompleta(EnterpriseProjectTrigger trigger,
                                    com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository enterpriseRepository,
                                    InMemoryDomainEventBus bus,
                                    ContextRepository durableContext,
                                    ProjectContext enterpriseContext) {
        when(durableContext.find(PROJECT_ID)).thenReturn(Optional.of(enterpriseContext));
        var service = new EnterpriseGenerationService(
            new DefaultBusinessModelEngine(), new DefaultMarketEngine(),
            new DefaultInnovationEngine(), new DefaultFinancialPlanEngine(),
            new DefaultRoadmapEngine(), new DefaultRiskPlanEngine(),
            new DefaultKpiEngine(), new DefaultEnterpriseScoreEngine(),
            new EnterpriseDocumentAssembler(), enterpriseRepository, bus, Runnable::run);
        var orchestrator = new EnterpriseGenerationOrchestrator(service);
        new EnterpriseProjectRequestedListener(orchestrator, durableContext, bus, Runnable::run);
        var context = EngineTestFixtures.contextWithAll();
        stubContexto(context);
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(new KinMethodResult(
            context, null, ConversationDecision.generateReport("informe"),
            "Aquí tenés el informe de viabilidad completo.", null, List.of(),
            ConsultingReport.empty()));
    }

    @Test
    void endToEnd_reportGeneraProyectoEnterpriseCompletado() {
        var enterpriseRepository = new InMemoryEnterpriseProjectRepository();
        var bus = new InMemoryDomainEventBus();
        var contextRepository = mock(ContextRepository.class);
        var trigger = new DefaultEnterpriseProjectTrigger(enterpriseRepository, bus);
        stubCadenaCompleta(trigger, enterpriseRepository, bus, contextRepository,
            EngineTestFixtures.contextWithAll());

        var result = orchestrator(trigger).orchestrate(turn());

        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        var generated = enterpriseRepository.findLatestVersion(PROJECT_ID).orElseThrow();
        assertTrue(generated.isCompleted());
        assertEquals(7, generated.documentCount());
        assertTrue(generated.hasDocument(DocumentType.LEAN_CANVAS));
        assertTrue(generated.hasDocument(DocumentType.KPI));
        long requested = bus.publishedEvents().stream()
            .filter(e -> e instanceof EnterpriseProjectRequested).count();
        long completed = bus.publishedEvents().stream()
            .filter(e -> e instanceof EnterpriseProjectGenerated).count();
        assertEquals(1, requested);
        assertEquals(1, completed);
    }

    @Test
    void endToEnd_reportGeneraProyectoDeFormaAsincrona() throws Exception {
        var enterpriseRepository = new InMemoryEnterpriseProjectRepository();
        var bus = new InMemoryDomainEventBus();
        var contextRepository = mock(ContextRepository.class);
        var trigger = new DefaultEnterpriseProjectTrigger(enterpriseRepository, bus);
        var service = new EnterpriseGenerationService(
            new DefaultBusinessModelEngine(), new DefaultMarketEngine(),
            new DefaultInnovationEngine(), new DefaultFinancialPlanEngine(),
            new DefaultRoadmapEngine(), new DefaultRiskPlanEngine(),
            new DefaultKpiEngine(), new DefaultEnterpriseScoreEngine(),
            new EnterpriseDocumentAssembler(), enterpriseRepository, bus, Runnable::run);
        var orchestrator = new EnterpriseGenerationOrchestrator(service);
        var executor = Executors.newSingleThreadExecutor();
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, bus, executor);
        when(contextRepository.find(PROJECT_ID)).thenReturn(Optional.of(EngineTestFixtures.contextWithAll()));
        var context = EngineTestFixtures.contextWithAll();
        stubContexto(context);
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(new KinMethodResult(
            context, null, ConversationDecision.generateReport("informe"),
            "Aquí tenés el informe de viabilidad completo.", null, List.of(),
            ConsultingReport.empty()));

        var result = orchestrator(trigger).orchestrate(turn());

        assertNotNull(result.aiResponse());
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var latest = enterpriseRepository.findLatestVersion(PROJECT_ID);
            if (latest.isPresent() && latest.get().isCompleted()) {
                executor.shutdownNow();
                assertTrue(true);
                return;
            }
            Thread.sleep(20);
        }
        executor.shutdownNow();
        throw new AssertionError("La generación asíncrona no completó el proyecto Enterprise");
    }

    @Test
    void turnoReport_conGeneracionEnVuelo_noPublicaSolicitudDuplicada() {
        var enterpriseRepository = new InMemoryEnterpriseProjectRepository();
        var now = java.time.OffsetDateTime.now();
        enterpriseRepository.save(EnterpriseProject.start(PROJECT_ID, 1, now, now, List.of()));
        var bus = new InMemoryDomainEventBus();
        var trigger = new DefaultEnterpriseProjectTrigger(enterpriseRepository, bus);
        stubReporte(ConsultingReport.empty());

        orchestrator(trigger).orchestrate(turn());

        assertTrue(bus.publishedEvents().stream()
            .noneMatch(e -> e instanceof EnterpriseProjectRequested));
    }
}
