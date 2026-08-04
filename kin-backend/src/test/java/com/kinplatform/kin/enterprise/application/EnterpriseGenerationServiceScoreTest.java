package com.kinplatform.kin.enterprise.application;

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
import com.kinplatform.kin.enterprise.events.EnterpriseProjectGenerated;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de la persistencia del Enterprise Score en la generación (Fase 10,
 * Milestone 3D): el servicio calcula el score con el motor existente y lo
 * adjunta al aggregate persistido (única fuente de verdad).
 */
class EnterpriseGenerationServiceScoreTest {

    private InMemoryEnterpriseProjectRepository repository;
    private InMemoryDomainEventBus eventBus;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnterpriseProjectRepository();
        eventBus = new InMemoryDomainEventBus();
    }

    @Test
    void generacionExitosa_persisteElEnterpriseScoreEnElAggregate() {
        var projectId = UUID.randomUUID();
        var service = realService();

        EnterpriseProject result = service.generate(request(projectId));

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertNotNull(result.score(), "El Enterprise Score debe adjuntarse al aggregate completado");
        assertTrue(result.score().overallScore() > 0);
        assertNotNull(result.score().grade());

        EnterpriseProject persisted = repository.findLatestVersion(projectId).orElseThrow();
        assertNotNull(persisted.score());
        assertEquals(result.score(), persisted.score());
        assertTrue(eventBus.publishedEvents().stream()
            .anyMatch(e -> e instanceof EnterpriseProjectGenerated));
    }

    @Test
    void generacionExitosa_sinDatosDePipeline_aunCalculaScoreParcial() {
        var projectId = UUID.randomUUID();
        var service = realService();
        var request = new EnterpriseGenerationRequest(
            projectId, EngineTestFixtures.contextWithAll(), null, null, null, null);

        EnterpriseProject result = service.generate(request);

        assertEquals(GenerationStatus.COMPLETED, result.status());
        assertNotNull(result.score());
        assertTrue(result.score().overallScore() >= 0);
    }

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
}
