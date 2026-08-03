package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseGenerationOrchestratorTest {

    @Test
    void constructorConServicioNulo_lanza() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseGenerationOrchestrator(null));
    }

    @Test
    void generate_delegaEnElServicio() {
        var service = mock(EnterpriseGenerationService.class);
        var orchestrator = new EnterpriseGenerationOrchestrator(service);
        var request = request();
        var expected = EnterpriseProject.request(request.projectId(), 1);
        when(service.generate(request)).thenReturn(expected);

        var result = orchestrator.generate(request);

        assertSame(expected, result);
        verify(service).generate(request);
    }

    @Test
    void generateAsync_delegaEnElServicio() {
        var service = mock(EnterpriseGenerationService.class);
        var orchestrator = new EnterpriseGenerationOrchestrator(service);
        var request = request();
        var future = new CompletableFuture<EnterpriseProject>();
        when(service.generateAsync(request)).thenReturn(future);

        var result = orchestrator.generateAsync(request);

        assertSame(future, result);
        verify(service).generateAsync(request);
    }

    @Test
    void generateHistorico_conSoloProjectId_lanzaUnsupportedOperation() {
        var service = mock(EnterpriseGenerationService.class);
        var orchestrator = new EnterpriseGenerationOrchestrator(service);

        assertThrows(UnsupportedOperationException.class,
            () -> orchestrator.generate(UUID.randomUUID()));
    }

    @Test
    void generate_devuelveElAggregatePersistido() {
        var service = mock(EnterpriseGenerationService.class);
        var orchestrator = new EnterpriseGenerationOrchestrator(service);
        var request = request();
        var expected = EnterpriseProject.request(request.projectId(), 1);
        when(service.generate(request)).thenReturn(expected);

        assertEquals(expected, orchestrator.generate(request));
    }

    private EnterpriseGenerationRequest request() {
        return new EnterpriseGenerationRequest(UUID.randomUUID(),
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8),
            EngineTestFixtures.riskResult(0.8));
    }
}
