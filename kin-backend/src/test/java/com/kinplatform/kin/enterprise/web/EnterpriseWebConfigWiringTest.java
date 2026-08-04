package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationRequest;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectRequestedListener;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de wiring del ciclo automático Enterprise (Fase 10, M3B): verifica que
 * {@link EnterpriseWebConfig} produce los tres beans nuevos (executor, trigger
 * y listener) invocando directamente los factory methods de la configuración,
 * sin levantar el contexto Spring completo (estrategia hermética del proyecto).
 */
class EnterpriseWebConfigWiringTest {

    private final EnterpriseWebConfig config = new EnterpriseWebConfig();

    @Test
    void enterpriseGenerationExecutor_devuelveUnExecutorQueEjecutaTareas() throws Exception {
        Executor executor = config.enterpriseGenerationExecutor();

        assertNotNull(executor);
        var latch = new CountDownLatch(1);
        executor.execute(latch::countDown);
        assertTrue(latch.await(2, TimeUnit.SECONDS), "El executor no ejecutó la tarea");
    }

    @Test
    void enterpriseProjectTrigger_publicaEnterpriseProjectRequestedEnElBus() {
        var projectId = UUID.randomUUID();
        var repository = mock(EnterpriseProjectRepository.class);
        when(repository.findLatestVersion(projectId)).thenReturn(Optional.empty());
        var eventBus = new InMemoryDomainEventBus();

        EnterpriseProjectTrigger trigger = config.enterpriseProjectTrigger(repository, eventBus);

        assertNotNull(trigger);
        trigger.request(projectId);

        var events = eventBus.publishedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested requested
            && requested.projectId().equals(projectId) && requested.version() == 1);
    }

    @Test
    void enterpriseProjectRequestedListener_seSuscribeYDelegaLaGeneracion() throws Exception {
        var projectId = UUID.randomUUID();
        var orchestrator = mock(EnterpriseGenerationOrchestrator.class);
        var contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(projectId)).thenReturn(Optional.of(EngineTestFixtures.contextWithAll()));
        var latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(orchestrator).generateRequested(any(), anyInt());
        var eventBus = new InMemoryDomainEventBus();

        EnterpriseProjectRequestedListener listener = config.enterpriseProjectRequestedListener(
            orchestrator, contextRepository, eventBus, config.enterpriseGenerationExecutor(),
            config.enterprisePipelineResultStore());

        assertNotNull(listener);
        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "La generación asíncrona no se ejecutó");
        verify(orchestrator).generateRequested(any(EnterpriseGenerationRequest.class), eq(1));
    }
}
