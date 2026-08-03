package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.aggregate.GenerationStatus;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.engine.BusinessModelEngine;
import com.kinplatform.kin.enterprise.engine.DefaultBusinessModelEngine;
import com.kinplatform.kin.enterprise.engine.DefaultEnterpriseScoreEngine;
import com.kinplatform.kin.enterprise.engine.DefaultFinancialPlanEngine;
import com.kinplatform.kin.enterprise.engine.DefaultInnovationEngine;
import com.kinplatform.kin.enterprise.engine.DefaultKpiEngine;
import com.kinplatform.kin.enterprise.engine.DefaultMarketEngine;
import com.kinplatform.kin.enterprise.engine.DefaultRiskPlanEngine;
import com.kinplatform.kin.enterprise.engine.DefaultRoadmapEngine;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.engine.EnterpriseScoreEngine;
import com.kinplatform.kin.enterprise.engine.FinancialPlanEngine;
import com.kinplatform.kin.enterprise.engine.InnovationEngine;
import com.kinplatform.kin.enterprise.engine.KpiEngine;
import com.kinplatform.kin.enterprise.engine.MarketEngine;
import com.kinplatform.kin.enterprise.engine.RiskPlanEngine;
import com.kinplatform.kin.enterprise.engine.RoadmapEngine;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectFailed;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectGenerated;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseGenerationServiceTest {

    private InMemoryEnterpriseProjectRepository repository;
    private InMemoryDomainEventBus eventBus;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnterpriseProjectRepository();
        eventBus = new InMemoryDomainEventBus();
    }

    // ------------------------------------------------------------------
    // Construcción de servicios y solicitudes de prueba
    // ------------------------------------------------------------------

    private EnterpriseGenerationService realService() {
        return new EnterpriseGenerationService(
            new DefaultBusinessModelEngine(), new DefaultMarketEngine(),
            new DefaultInnovationEngine(), new DefaultFinancialPlanEngine(),
            new DefaultRoadmapEngine(), new DefaultRiskPlanEngine(),
            new DefaultKpiEngine(), new DefaultEnterpriseScoreEngine(),
            new EnterpriseDocumentAssembler(), repository, eventBus, Runnable::run);
    }

    private EnterpriseGenerationRequest request(UUID projectId) {
        return new EnterpriseGenerationRequest(projectId, EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8), EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8), EngineTestFixtures.riskResult(0.8));
    }

    private EnterpriseGenerationRequest request() {
        return request(UUID.randomUUID());
    }

    // ------------------------------------------------------------------
    // Generación exitosa
    // ------------------------------------------------------------------

    @Test
    void generacionExitosa_creaProyectoCompletadoConDocumentos() {
        var projectId = UUID.randomUUID();

        var result = realService().generate(request(projectId));

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertEquals(1, result.version());
        assertTrue(result.isCompleted());
        assertNotNull(result.completedAt());
        assertNull(result.failedReason());
        assertEquals(7, result.documentCount());
        assertTrue(result.hasDocument(DocumentType.LEAN_CANVAS));
        assertTrue(result.hasDocument(DocumentType.MARKET_PLAN));
        assertTrue(result.hasDocument(DocumentType.INNOVATION_PLAN));
        assertTrue(result.hasDocument(DocumentType.FINANCIAL_PLAN));
        assertTrue(result.hasDocument(DocumentType.ROADMAP));
        assertTrue(result.hasDocument(DocumentType.RISK_MATRIX));
        assertTrue(result.hasDocument(DocumentType.KPI));

        var persisted = repository.findLatestVersion(projectId).orElseThrow();
        assertEquals(GenerationStatus.COMPLETED, persisted.status());
    }

    @Test
    void generacionExitosa_emiteEventosRequestedYGeneratedEnOrden() {
        var projectId = UUID.randomUUID();

        realService().generate(request(projectId));

        var events = eventBus.publishedEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested requested
            && requested.projectId().equals(projectId) && requested.version() == 1);
        assertTrue(events.get(1) instanceof EnterpriseProjectGenerated generated
            && generated.projectId().equals(projectId) && generated.version() == 1);
    }

    @Test
    void segundaGeneracion_desdeCompletado_creaVersionSiguiente() {
        var service = realService();
        var projectId = UUID.randomUUID();

        service.generate(request(projectId));
        var second = service.generate(request(projectId));

        assertEquals(2, second.version());
        assertEquals(GenerationStatus.COMPLETED, second.status());
        assertEquals(2, repository.findAllVersions(projectId).size());
        var events = eventBus.publishedEvents();
        assertEquals(4, events.size());
        assertTrue(events.get(2) instanceof EnterpriseProjectRequested requested
            && requested.version() == 2);
    }

    @Test
    void generacion_desdeFallido_creaVersionSiguiente() {
        var service = realService();
        var projectId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        repository.save(EnterpriseProject.fail(projectId, 1, now, now, "motivo previo", List.of()));

        var result = service.generate(request(projectId));

        assertEquals(2, result.version());
        assertEquals(GenerationStatus.COMPLETED, result.status());
    }

    // ------------------------------------------------------------------
    // Idempotencia
    // ------------------------------------------------------------------

    @Test
    void generacion_conVersionRequestedEnVuelo_esIdempotente() {
        var service = realService();
        var projectId = UUID.randomUUID();
        var requested = EnterpriseProject.request(projectId, 1);
        repository.save(requested);

        var result = service.generate(request(projectId));

        assertSame(requested, result);
        assertEquals(GenerationStatus.REQUESTED, result.status());
        assertTrue(eventBus.publishedEvents().isEmpty());
        assertEquals(1, repository.findAllVersions(projectId).size());
    }

    @Test
    void generacion_conVersionRunningEnVuelo_esIdempotente() {
        var service = realService();
        var projectId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var running = EnterpriseProject.start(projectId, 1, now, now, List.of());
        repository.save(running);

        var result = service.generate(request(projectId));

        assertSame(running, result);
        assertEquals(GenerationStatus.RUNNING, result.status());
        assertTrue(eventBus.publishedEvents().isEmpty());
    }

    // ------------------------------------------------------------------
    // Fallo
    // ------------------------------------------------------------------

    @Test
    void falloDeMotor_creaProyectoFallidoYEmiteEvento() {
        var projectId = UUID.randomUUID();
        var businessModel = mock(BusinessModelEngine.class);
        when(businessModel.evaluate(any())).thenThrow(new IllegalStateException("boom"));

        var service = new EnterpriseGenerationService(businessModel,
            new DefaultMarketEngine(), new DefaultInnovationEngine(),
            new DefaultFinancialPlanEngine(), new DefaultRoadmapEngine(),
            new DefaultRiskPlanEngine(), new DefaultKpiEngine(),
            new DefaultEnterpriseScoreEngine(), new EnterpriseDocumentAssembler(),
            repository, eventBus, Runnable::run);

        var result = service.generate(request(projectId));

        assertEquals(GenerationStatus.FAILED, result.status());
        assertEquals(1, result.version());
        assertEquals("boom", result.failedReason());
        assertNotNull(result.failedReason());

        var events = eventBus.publishedEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested);
        assertTrue(events.get(1) instanceof EnterpriseProjectFailed failed
            && failed.projectId().equals(projectId)
            && failed.version() == 1
            && failed.reason().equals("boom"));

        var persisted = repository.findLatestVersion(projectId).orElseThrow();
        assertEquals(GenerationStatus.FAILED, persisted.status());
    }

    @Test
    void falloConMensajeNulo_usaNombreDeClaseComoMotivo() {
        var projectId = UUID.randomUUID();
        var businessModel = mock(BusinessModelEngine.class);
        when(businessModel.evaluate(any())).thenThrow(new RuntimeException());

        var service = new EnterpriseGenerationService(businessModel,
            new DefaultMarketEngine(), new DefaultInnovationEngine(),
            new DefaultFinancialPlanEngine(), new DefaultRoadmapEngine(),
            new DefaultRiskPlanEngine(), new DefaultKpiEngine(),
            new DefaultEnterpriseScoreEngine(), new EnterpriseDocumentAssembler(),
            repository, eventBus, Runnable::run);

        var result = service.generate(request(projectId));

        assertEquals("RuntimeException", result.failedReason());
    }

    // ------------------------------------------------------------------
    // Coordinación de los ocho motores
    // ------------------------------------------------------------------

    @Test
    void generacion_ejecutaLosOchoMotoresEnOrdenDeDependencia() {
        var businessModel = mock(BusinessModelEngine.class);
        var market = mock(MarketEngine.class);
        var innovation = mock(InnovationEngine.class);
        var financial = mock(FinancialPlanEngine.class);
        var roadmap = mock(RoadmapEngine.class);
        var risk = mock(RiskPlanEngine.class);
        var kpi = mock(KpiEngine.class);
        var score = mock(EnterpriseScoreEngine.class);

        var service = new EnterpriseGenerationService(businessModel, market,
            innovation, financial, roadmap, risk, kpi, score,
            new EnterpriseDocumentAssembler(), repository, eventBus, Runnable::run);

        service.generate(request());

        var order = inOrder(businessModel, market, innovation, financial, roadmap, risk, kpi, score);
        order.verify(businessModel).evaluate(any());
        order.verify(market).evaluate(any());
        order.verify(innovation).evaluate(any());
        order.verify(financial).evaluate(any());
        order.verify(roadmap).evaluate(any());
        order.verify(risk).evaluate(any());
        order.verify(kpi).evaluate(any());
        order.verify(score).evaluate(any());
        verify(businessModel, never()).metadata();
    }

    @Test
    void generacion_sinPipeline_usaResultadosVaciosSinFallar() {
        var projectId = UUID.randomUUID();
        var request = new EnterpriseGenerationRequest(
            projectId, EngineTestFixtures.contextWithAll(), null, null, null, null);

        var result = realService().generate(request);

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertEquals(7, result.documentCount());
        assertTrue(eventBus.publishedEvents().get(1) instanceof EnterpriseProjectGenerated);
    }

    // ------------------------------------------------------------------
    // Asíncrono
    // ------------------------------------------------------------------

    @Test
    void generacionAsincrona_completaElProyecto() throws Exception {
        var projectId = UUID.randomUUID();

        CompletableFuture<EnterpriseProject> future = realService().generateAsync(request(projectId));

        var result = future.get();
        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertEquals(1, result.version());
        var events = eventBus.publishedEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(1) instanceof EnterpriseProjectGenerated);
    }

    @Test
    void generacionAsincrona_conMotorFallido_devuelveProyectoFallido() throws Exception {
        var projectId = UUID.randomUUID();
        var businessModel = mock(BusinessModelEngine.class);
        when(businessModel.evaluate(any())).thenThrow(new IllegalStateException("fallo async"));

        var service = new EnterpriseGenerationService(businessModel,
            new DefaultMarketEngine(), new DefaultInnovationEngine(),
            new DefaultFinancialPlanEngine(), new DefaultRoadmapEngine(),
            new DefaultRiskPlanEngine(), new DefaultKpiEngine(),
            new DefaultEnterpriseScoreEngine(), new EnterpriseDocumentAssembler(),
            repository, eventBus, Runnable::run);

        var result = service.generateAsync(request(projectId)).get();

        assertEquals(GenerationStatus.FAILED, result.status());
        assertEquals("fallo async", result.failedReason());
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    @Test
    void generateConSolicitudNula_lanza() {
        var service = realService();
        assertThrows(IllegalArgumentException.class, () -> service.generate(null));
        assertThrows(IllegalArgumentException.class, () -> service.generateAsync(null));
    }

    @Test
    void constructorDeConveniencia_conMotoresPorDefecto_generaCompletado() {
        var service = new EnterpriseGenerationService(
            new EnterpriseDocumentAssembler(), repository, eventBus);

        var result = service.generate(request());

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertEquals(7, result.documentCount());
        assertEquals(2, eventBus.publishedEvents().size());
    }

    @Test
    void constructorConDependenciaNula_lanza() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseGenerationService(
            null, new DefaultMarketEngine(), new DefaultInnovationEngine(),
            new DefaultFinancialPlanEngine(), new DefaultRoadmapEngine(),
            new DefaultRiskPlanEngine(), new DefaultKpiEngine(),
            new DefaultEnterpriseScoreEngine(), new EnterpriseDocumentAssembler(),
            repository, eventBus, Runnable::run));
    }
}
