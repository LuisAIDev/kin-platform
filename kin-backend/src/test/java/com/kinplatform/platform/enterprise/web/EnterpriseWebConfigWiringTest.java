package com.kinplatform.platform.enterprise.web;

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

import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.platform.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.platform.enterprise.application.EnterpriseGenerationRequest;
import com.kinplatform.platform.enterprise.application.EnterpriseProjectRequestedListener;
import com.kinplatform.platform.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.platform.enterprise.engine.EngineTestFixtures;
import com.kinplatform.platform.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.platform.enterprise.ports.EnterpriseProjectRepository;
import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.common.eventbus.IdempotencyService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.Mockito;

class EnterpriseWebConfigWiringTest {

    private final EnterpriseWebConfig config = new EnterpriseWebConfig();
    private final IdempotencyService idempotencyService = new IdempotencyService(Mockito.mock(JdbcTemplate.class));

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
                && requested.projectId().equals(projectId)
                && requested.version() == 1);
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
                })
                .when(orchestrator)
                .generateRequested(any(), anyInt());
        var eventBus = new InMemoryDomainEventBus();

        EnterpriseProjectRequestedListener listener = config.enterpriseProjectRequestedListener(
                orchestrator,
                contextRepository,
                eventBus,
                config.enterpriseGenerationExecutor(),
                config.enterprisePipelineResultStore(),
                null,
                idempotencyService);

        assertNotNull(listener);
        eventBus.publish(new EnterpriseProjectRequested(projectId, 1));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "La generación asíncrona no se ejecutó");
        verify(orchestrator).generateRequested(any(EnterpriseGenerationRequest.class), eq(1));
    }
}




