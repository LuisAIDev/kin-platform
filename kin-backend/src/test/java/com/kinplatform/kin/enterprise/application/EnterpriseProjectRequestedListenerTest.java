package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseProjectRequestedListenerTest {

    private final InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();

    @Test
    void onRequested_conContextoDelegaEnElOrquestador() {
        var projectId = UUID.randomUUID();
        var context = EngineTestFixtures.contextWithAll();
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(context));
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, Runnable::run);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        ArgumentCaptor<EnterpriseGenerationRequest> captor =
            ArgumentCaptor.forClass(EnterpriseGenerationRequest.class);
        verify(orchestrator).generateRequested(captor.capture(), org.mockito.ArgumentMatchers.eq(1));
        var request = captor.getValue();
        assertEquals(projectId, request.projectId());
        assertEquals(context, request.context());
        assertTrue(request.recommendations().isEmpty());
        assertTrue(request.opportunities().isEmpty());
        assertTrue(request.knowledge().isEmpty());
        assertTrue(request.riskResult().isEmpty());
    }

    @Test
    void onRequested_sinContexto_noDelega() {
        var projectId = UUID.randomUUID();
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.empty());
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, Runnable::run);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        verify(orchestrator, never()).generateRequested(any(), anyInt());
    }

    @Test
    void onRequested_conEventoNulo_noLanza() {
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        var listener = new EnterpriseProjectRequestedListener(
            orchestrator, contextRepository, eventBus, Runnable::run);

        listener.onRequested(null);

        verify(orchestrator, never()).generateRequested(any(), anyInt());
    }

    @Test
    void onRequested_generacionLanza_noPropaga() {
        var projectId = UUID.randomUUID();
        var context = EngineTestFixtures.contextWithAll();
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        doAnswer(inv -> {
            throw new IllegalStateException("fallo generacion");
        }).when(orchestrator).generateRequested(any(), anyInt());
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(context));
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, Runnable::run);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));
    }

    @Test
    void onRequested_seEjecutaDeFormaAsincrona() throws Exception {
        var projectId = UUID.randomUUID();
        var context = EngineTestFixtures.contextWithAll();
        var latch = new CountDownLatch(1);
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(orchestrator).generateRequested(any(), anyInt());
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(context));
        var executor = Executors.newSingleThreadExecutor();
        new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, executor);

        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "La generación asíncrona no se ejecutó");
        executor.shutdownNow();
    }

    @Test
    void constructorConDependenciaNula_lanza() {
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectRequestedListener(null, contextRepository, eventBus, Runnable::run));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectRequestedListener(orchestrator, null, eventBus, Runnable::run));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectRequestedListener(orchestrator, contextRepository, null, Runnable::run));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectRequestedListener(orchestrator, contextRepository, eventBus, null));
    }
}
