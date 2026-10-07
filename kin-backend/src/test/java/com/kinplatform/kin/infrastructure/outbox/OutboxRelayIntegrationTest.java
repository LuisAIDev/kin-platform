package com.kinplatform.platform.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.ReportGeneratedEvent;
import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import com.kinplatform.common.eventbus.domain.OutboxRecord;
import com.kinplatform.common.eventbus.domain.OutboxStatus;
import com.kinplatform.platform.infrastructure.outbox.OutboxRecordRowMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class OutboxRelayIntegrationTest extends com.kinplatform.test.PostgresTestSupport {

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxEventPublisher publisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private DomainEventBus domainEventBus;

    @Autowired
    private MeterRegistry meterRegistry;

    private static final String TEST_EVENT_TYPE = "com.kinplatform.common.event.ReportGeneratedEvent";

    @BeforeEach
    void setUp() {
        // Limpiar tabla antes de cada test
        jdbcTemplate.update("DELETE FROM domain_event_outbox");
        Mockito.clearInvocations(domainEventBus);
    }

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(domainEventBus);
    }

    @Test
    @DisplayName("Publicación atómica y procesamiento: evento se publica y marca PUBLISHED")
    void publishAndProcess_eventIsPublishedAndMarked() {
        UUID projectId = UUID.randomUUID();
        var event = new ReportGeneratedEvent(projectId, "PDF");

        // Publicar dentro de una transacción (simula caso de uso real)
        transactionTemplate.execute(status -> {
            publisher.publish(event);
            return null;
        });

        // Verificar que se insertó en outbox
        int pendingBefore = countPending();
        assertThat(pendingBefore).isEqualTo(1);

        // Ejecutar relé manualmente
        relay.processPendingEvents();

        // Verificar que el bus recibió el evento
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventBus, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ReportGeneratedEvent.class);
        assertThat(((ReportGeneratedEvent) eventCaptor.getValue()).aggregateId()).isEqualTo(projectId);

        // Verificar que el registro se marcó PUBLISHED
        var record = getOutboxRecord();
        assertThat(record.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(record.publishedAt()).isNotNull();

        // Verificar métricas
        assertThat(getMetric("kin.outbox.published")).isEqualTo(1.0);
        assertThat(getMetric("kin.outbox.pending")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Reintentos: evento fallido se reintenta y tras maxRetries pasa a DEAD_LETTER")
    void failedEvent_retriesAndThenDeadLetter() {
        // Configurar el bus para que falle siempre
        doThrow(new RuntimeException("Simulated bus failure"))
                .when(domainEventBus).publish(any(DomainEvent.class));

        UUID projectId = UUID.randomUUID();
        var event = new ReportGeneratedEvent(projectId, "PDF");

        transactionTemplate.execute(status -> {
            publisher.publish(event);
            return null;
        });

        int maxRetries = 3; // Configurado en test profile
        int expectedFailures = maxRetries;

        // Ejecutar relé múltiples veces (cada llamada procesa el lote)
        for (int i = 1; i <= maxRetries + 1; i++) {
            relay.processPendingEvents();
        }

        // Verificar que pasó a DEAD_LETTER
        var record = getOutboxRecord();
        assertThat(record.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(record.retryCount()).isEqualTo(maxRetries);

        // Verificar métricas
        assertThat(getMetric("kin.outbox.dead_letter")).isEqualTo(1.0);
        assertThat(getMetric("kin.outbox.failed")).isEqualTo((double) expectedFailures);
    }

    @Test
    @DisplayName("Métricas: contadores y gauge se actualizan correctamente")
    void metrics_areUpdatedCorrectly() {
        UUID projectId = UUID.randomUUID();
        var event = new ReportGeneratedEvent(projectId, "PDF");

        transactionTemplate.execute(status -> {
            publisher.publish(event);
            return null;
        });

        // Antes del relé
        assertThat(getMetric("kin.outbox.pending")).isEqualTo(1.0);
        assertThat(getMetric("kin.outbox.published")).isEqualTo(0.0);

        relay.processPendingEvents();

        // Después del relé
        assertThat(getMetric("kin.outbox.pending")).isEqualTo(0.0);
        assertThat(getMetric("kin.outbox.published")).isEqualTo(1.0);

        // Verificar timer existe
        assertThat(meterRegistry.find("kin.outbox.relay.duration").timer()).isNotNull();
    }

    @Test
    @DisplayName("Concurrencia: dos instancias no procesan el mismo registro (SKIP LOCKED)")
    void concurrency_twoRelaysDoNotProcessSameRecord() throws InterruptedException {
        UUID projectId = UUID.randomUUID();
        var event = new ReportGeneratedEvent(projectId, "PDF");

        transactionTemplate.execute(status -> {
            publisher.publish(event);
            return null;
        });

        // Ejecutar dos relés en paralelo simulando dos instancias
        Thread t1 = new Thread(() -> relay.processPendingEvents());
        Thread t2 = new Thread(() -> relay.processPendingEvents());

        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);

        // Solo uno debería haber procesado el evento
        var record = getOutboxRecord();
        assertThat(record.status()).isEqualTo(OutboxStatus.PUBLISHED);
        // Solo una publicación en el bus
        verify(domainEventBus, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Desactivado: con relay.enabled=false no procesa nada")
    void disabled_relayDoesNotProcess() {
        // Este test requiere un contexto con relay.enabled=false
        // Se valida en OutboxRelayDisabledIntegrationTest (perfil test por defecto)
    }

    // Helpers

    private int countPending() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_event_outbox WHERE status = 'PENDING'", Integer.class);
    }

    private OutboxRecord getOutboxRecord() {
        return jdbcTemplate.queryForObject(
                "SELECT id, aggregate_id, event_type, payload, metadata, status, retry_count, " +
                        "created_at, published_at, last_error " +
                        "FROM domain_event_outbox ORDER BY created_at DESC LIMIT 1",
                new OutboxRecordRowMapper());
    }

    private double getMetric(String name) {
        return meterRegistry.find(name).counter().count();
    }
}



