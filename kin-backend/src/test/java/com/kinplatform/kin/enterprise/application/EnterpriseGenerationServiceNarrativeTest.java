package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.aggregate.GenerationStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la generación narrativa en el servicio (Fase 10, Milestone 3E): con
 * el puerto {@code AIResponder} inyectado, la generación produce 9 documentos
 * (7 deterministas + EXECUTIVE_REPORT + DOFA); sin IA, conserva los 7
 * deterministas (compatibilidad).
 */
class EnterpriseGenerationServiceNarrativeTest {

    private InMemoryEnterpriseProjectRepository repository;
    private InMemoryDomainEventBus eventBus;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnterpriseProjectRepository();
        eventBus = new InMemoryDomainEventBus();
    }

    @Test
    void generacionConIa_produceLos9DocumentosConNarrativa() {
        var projectId = UUID.randomUUID();
        var aiResponder = mock(AIResponder.class);
        when(aiResponder.respond(any(AIRequest.class)))
            .thenReturn("Executive Report narrado por la IA", "DOFA narrado por la IA");

        EnterpriseProject result = service(aiResponder).generate(request(projectId));

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertEquals(9, result.documentCount());
        assertTrue(result.hasDocument(DocumentType.EXECUTIVE_REPORT));
        assertTrue(result.hasDocument(DocumentType.DOFA));
        assertEquals("Executive Report narrado por la IA",
            result.findDocument(DocumentType.EXECUTIVE_REPORT).orElseThrow().content());
        assertEquals("DOFA narrado por la IA",
            result.findDocument(DocumentType.DOFA).orElseThrow().content());
        verify(aiResponder, org.mockito.Mockito.times(2)).respond(any(AIRequest.class));
    }

    @Test
    void generacionSinIa_conservaLos7DocumentosDeterministas() {
        var projectId = UUID.randomUUID();

        EnterpriseProject result = service(null).generate(request(projectId));

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertEquals(7, result.documentCount());
        assertTrue(result.hasDocument(DocumentType.LEAN_CANVAS));
        assertTrue(result.hasDocument(DocumentType.KPI));
    }

    @Test
    void generacionConIa_enBlanco_usaFallbackDeterministaEnLosDosDocumentos() {
        var projectId = UUID.randomUUID();
        var aiResponder = mock(AIResponder.class);
        when(aiResponder.respond(any(AIRequest.class))).thenReturn(" ");

        EnterpriseProject result = service(aiResponder).generate(request(projectId));

        assertEquals(9, result.documentCount());
        assertTrue(result.findDocument(DocumentType.EXECUTIVE_REPORT).orElseThrow()
            .content().contains("Resumen Ejecutivo"));
        assertTrue(result.findDocument(DocumentType.DOFA).orElseThrow()
            .content().contains("Fortalezas"));
    }

    private EnterpriseGenerationService service(AIResponder aiResponder) {
        return new EnterpriseGenerationService(
            new DefaultBusinessModelEngine(), new DefaultMarketEngine(),
            new DefaultInnovationEngine(), new DefaultFinancialPlanEngine(),
            new DefaultRoadmapEngine(), new DefaultRiskPlanEngine(),
            new DefaultKpiEngine(), new DefaultEnterpriseScoreEngine(),
            new EnterpriseDocumentAssembler(), repository, eventBus, Runnable::run, aiResponder);
    }

    private EnterpriseGenerationRequest request(UUID projectId) {
        return new EnterpriseGenerationRequest(projectId, EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8), EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8), EngineTestFixtures.riskResult(0.8));
    }
}
