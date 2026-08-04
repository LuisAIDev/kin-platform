package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la fusión de los resultados reales del pipeline en la
 * {@code EnterpriseGenerationRequest} (Fase 10, Milestone 3C): el listener
 * recupera los resultados del turno {@code REPORT} del
 * {@link EnterprisePipelineResultStore} y los entrega a la generación.
 */
class EnterpriseProjectRequestedListenerPipelineResultsTest {

    private final InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();

    @Test
    void onRequested_conResultadosEnElStore_construyeLaRequestConDatosReales() {
        var projectId = UUID.randomUUID();
        var context = EngineTestFixtures.contextWithAll();
        var store = new InMemoryEnterprisePipelineResultStore();
        store.store(new EnterpriseTurnResults(projectId,
            EngineTestFixtures.recommendations(0.8), EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8), EngineTestFixtures.riskResult(0.8)));
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(context));
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, Runnable::run, store);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        ArgumentCaptor<EnterpriseGenerationRequest> captor =
            ArgumentCaptor.forClass(EnterpriseGenerationRequest.class);
        verify(orchestrator).generateRequested(captor.capture(), eq(1));
        var request = captor.getValue();
        assertFalse(request.recommendations().isEmpty());
        assertFalse(request.opportunities().isEmpty());
        assertFalse(request.knowledge().isEmpty());
        assertFalse(request.riskResult().isEmpty());
        assertEquals(context, request.context());
    }

    @Test
    void onRequested_sinStore_operaOfflineConResultadosVacios() {
        var projectId = UUID.randomUUID();
        var context = EngineTestFixtures.contextWithAll();
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(context));
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, Runnable::run);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        ArgumentCaptor<EnterpriseGenerationRequest> captor =
            ArgumentCaptor.forClass(EnterpriseGenerationRequest.class);
        verify(orchestrator).generateRequested(captor.capture(), eq(1));
        var request = captor.getValue();
        assertTrue(request.recommendations().isEmpty());
        assertTrue(request.opportunities().isEmpty());
        assertTrue(request.knowledge().isEmpty());
        assertTrue(request.riskResult().isEmpty());
    }

    @Test
    void onRequested_conStoreVacio_operaOffline() {
        var projectId = UUID.randomUUID();
        var context = EngineTestFixtures.contextWithAll();
        var store = new InMemoryEnterprisePipelineResultStore();
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(context));
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, Runnable::run, store);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        ArgumentCaptor<EnterpriseGenerationRequest> captor =
            ArgumentCaptor.forClass(EnterpriseGenerationRequest.class);
        verify(orchestrator).generateRequested(captor.capture(), eq(1));
        var request = captor.getValue();
        assertTrue(request.recommendations().isEmpty());
        assertTrue(request.knowledge().isEmpty());
    }
}
