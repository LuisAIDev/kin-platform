package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.progress.EnterpriseProgressEvent;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnterpriseProgressServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private ScheduledExecutorService executor;
    private EnterpriseProgressService service;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "test-progress-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        service = new EnterpriseProgressService(executor, 60_000L);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void subscribe_deberiaRegistrarUnCliente() {
        var emitter = service.subscribe(PROJECT_ID, 1);

        assertNotNull(emitter);
        assertEquals(1, service.activeSubscriptionCount());
    }

    @Test
    void publish_conClienteSuscrito_deberiaMantenerLaSuscripcion() {
        service.subscribe(PROJECT_ID, 1);

        service.publish(event(EnterpriseProgressState.RUNNING));

        assertEquals(1, service.activeSubscriptionCount());
    }

    @Test
    void publish_terminal_deberiaCompletarYCerrarLaSuscripcion() {
        service.subscribe(PROJECT_ID, 1);

        service.publish(event(EnterpriseProgressState.COMPLETED));

        assertEquals(0, service.activeSubscriptionCount());
    }

    @Test
    void publish_sinClientes_deberiaSerInofensivo() {
        service.publish(event(EnterpriseProgressState.RUNNING));
        assertEquals(0, service.activeSubscriptionCount());
    }

    @Test
    void publish_terminalFallido_deberiaCompletarYCerrarLaSuscripcion() {
        service.subscribe(PROJECT_ID, 1);

        service.publish(event(EnterpriseProgressState.FAILED));

        assertEquals(0, service.activeSubscriptionCount());
    }

    @Test
    void publish_conEventoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> service.publish(null));
    }

    @Test
    void subscribe_conProjectIdNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> service.subscribe(null, 1));
    }

    @Test
    void subscribe_conVersionInvalida_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> service.subscribe(PROJECT_ID, 0));
    }

    @Test
    void constructor_conDependenciaInvalida_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProgressService(null, 1000L));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProgressService(executor, 0L));
    }

    @Test
    void publicacion_conVariasVersiones_deberiaAislarLosSuscriptores() {
        service.subscribe(PROJECT_ID, 1);
        service.subscribe(PROJECT_ID, 2);

        service.publish(EnterpriseProgressEvent.of(PROJECT_ID, 1,
            EnterpriseProgressState.COMPLETED, null, "completado"));

        assertEquals(1, service.activeSubscriptionCount());
    }

    @Test
    void subscribe_conHeartbeatCorto_deberiaEjecutarLaTareaDeHeartbeat() throws Exception {
        var shortService = new EnterpriseProgressService(executor, 20L);
        shortService.subscribe(PROJECT_ID, 1);

        Thread.sleep(80);

        shortService.publish(event(EnterpriseProgressState.COMPLETED));
        assertEquals(0, shortService.activeSubscriptionCount());
    }

    @Test
    void publish_conEmisorCerrado_deberiaLimpiarLaSuscripcion() {
        var emitter = service.subscribe(PROJECT_ID, 1);

        emitter.complete();
        service.publish(event(EnterpriseProgressState.RUNNING));

        assertEquals(0, service.activeSubscriptionCount());
    }

    private EnterpriseProgressEvent event(EnterpriseProgressState state) {
        return EnterpriseProgressEvent.of(PROJECT_ID, 1, state, null, "evento");
    }
}
