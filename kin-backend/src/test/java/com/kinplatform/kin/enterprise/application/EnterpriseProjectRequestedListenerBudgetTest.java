package com.kinplatform.kin.enterprise.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifica que el listener Enterprise respeta el gate de presupuesto: sin
 * presupuesto suficiente NO se invoca a la generación (y por tanto NO se llega
 * a DeepSeek); con presupuesto, la generación se ejecuta y el contexto de
 * reserva se limpia.
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseProjectRequestedListenerBudgetTest {

    @Mock
    private EnterpriseGenerationOrchestrator orchestrator;

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private EnterpriseAiBudgetGate aiBudgetGate;

    private final UUID projectId = UUID.randomUUID();

    private Executor directExecutor() {
        return Runnable::run;
    }

    private EnterpriseProjectRequestedListener listener(DomainEventBus bus) {
        return new EnterpriseProjectRequestedListener(
                orchestrator,
                contextRepository,
                bus,
                directExecutor(),
                new EnterprisePipelineResultStore() {
                    @Override
                    public void store(EnterpriseTurnResults results) {}

                    @Override
                    public Optional<EnterpriseTurnResults> consume(UUID id) {
                        return Optional.empty();
                    }
                },
                aiBudgetGate);
    }

    @Test
    void sinPresupuesto_noEjecutaGeneracion() {
        when(aiBudgetGate.reserve(projectId)).thenReturn(false);
        var bus = new InMemoryDomainEventBus();
        var listener = listener(bus);
        assertNotNull(listener);

        bus.publish(new EnterpriseProjectRequested(projectId, 1));

        verify(orchestrator, never()).generateRequested(any(), anyInt());
        verify(aiBudgetGate).clear();
    }

    @Test
    void conPresupuesto_ejecutaYlimpiaContexto() {
        when(aiBudgetGate.reserve(projectId)).thenReturn(true);
        when(contextRepository.find(projectId))
                .thenReturn(Optional.of(ProjectContext.fromProject("P", "D", "Software")));
        var bus = new InMemoryDomainEventBus();
        var listener = listener(bus);
        assertNotNull(listener);

        bus.publish(new EnterpriseProjectRequested(projectId, 1));

        verify(orchestrator).generateRequested(any(), anyInt());
        verify(aiBudgetGate).clear();
    }
}
