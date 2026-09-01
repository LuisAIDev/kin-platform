package com.kinplatform.kin.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.eventbus.domain.OutboxRecord;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "kin.outbox.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class OutboxRelay {

    private final JdbcTemplate jdbcTemplate;
    private final DomainEventBus domainEventBus;
    private final ObjectMapper objectMapper;
    private final OutboxRelayProperties properties;
    private final MeterRegistry meterRegistry;

    private final Counter publishedCounter;
    private final Counter failedCounter;
    private final Counter deadLetterCounter;
    private final Timer relayDurationTimer;

    @Autowired
    public OutboxRelay(
            JdbcTemplate jdbcTemplate,
            DomainEventBus domainEventBus,
            ObjectMapper objectMapper,
            OutboxRelayProperties properties,
            MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainEventBus = domainEventBus;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.meterRegistry = meterRegistry;

        this.publishedCounter = Counter.builder("kin.outbox.published")
                .description("Number of events successfully published from outbox")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("kin.outbox.failed")
                .description("Number of events that failed to publish (will be retried)")
                .register(meterRegistry);

        this.deadLetterCounter = Counter.builder("kin.outbox.dead_letter")
                .description("Number of events moved to dead letter queue")
                .register(meterRegistry);

        this.relayDurationTimer = Timer.builder("kin.outbox.relay.duration")
                .description("Duration of the outbox relay processing cycle")
                .register(meterRegistry);

        // Gauge for pending events count
        Gauge.builder("kin.outbox.pending", this, relay -> countPending())
                .description("Current number of pending events in outbox")
                .register(meterRegistry);
    }

    /**
     * Procesa eventos pendientes del outbox.
     * Se ejecuta cada {@code kin.outbox.relay.poll-interval-ms} milisegundos.
     */
    @Scheduled(fixedDelayString = "${kin.outbox.relay.poll-interval-ms:2000}")
    @Transactional
    public void processPendingEvents() {
        if (!properties.isEnabled()) {
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        int processed = 0;

        try {
            List<OutboxRecord> batch = fetchPendingBatch();
            if (batch.isEmpty()) {
                log.debug("No hay eventos pendientes en el outbox");
                return;
            }

            log.debug("Procesando lote de {} eventos del outbox", batch.size());

            for (OutboxRecord record : batch) {
                try {
                    processRecord(record);
                    processed++;
                } catch (Exception e) {
                    handleFailure(record, e);
                }
            }

            log.debug("Lote procesado: {} eventos publicados exitosamente", processed);

        } finally {
            sample.stop(relayDurationTimer);
        }
    }

    /**
     * Obtiene un lote de eventos pendientes con bloqueo optimista (SKIP LOCKED).
     */
    private List<OutboxRecord> fetchPendingBatch() {
        String sql =
                """
            SELECT id, aggregate_id, event_type, payload, metadata, status, retry_count,
                   created_at, published_at, last_error
            FROM domain_event_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT ? FOR UPDATE SKIP LOCKED
            """;

        return jdbcTemplate.query(sql, new OutboxRecordRowMapper(), properties.getBatchSize());
    }

    /**
     * Procesa un registro individual: deserializa, publica en el bus, marca como publicado.
     */
    private void processRecord(OutboxRecord record) {
        DomainEvent event;
        try {
            event = objectMapper.readValue(record.payload(), DomainEvent.class);
        } catch (Exception e) {
            log.error(
                    "Error deserializando evento del outbox: id={}, aggregateId={}, error={}",
                    record.id(),
                    record.aggregateId(),
                    e.getMessage(),
                    e);
            // Error de deserialización no recuperable -> DEAD_LETTER
            markDeadLetter(record, "Error deserializando: " + e.getMessage());
            return;
        }

        // Extraer correlationId y userId del metadata para logging
        String correlationId = extractMetadataValue(record.metadata(), "correlationId");
        String userId = extractMetadataValue(record.metadata(), "userId");

        log.debug(
                "Publicando evento: id={}, type={}, aggregateId={}, correlationId={}, userId={}",
                record.id(),
                record.eventType(),
                record.aggregateId(),
                correlationId,
                userId);

        // Publicar en el bus de eventos
        domainEventBus.publish(event);

        // Marcar como publicado
        markPublished(record);
        publishedCounter.increment();

        log.debug("Evento publicado exitosamente: id={}, aggregateId={}", record.id(), record.aggregateId());
    }

    /**
     * Maneja un fallo en el procesamiento: incrementa contador, actualiza status.
     */
    private void handleFailure(OutboxRecord record, Exception e) {
        int newRetryCount = record.retryCount() + 1;
        String errorMessage = truncateError(e.getMessage());

        if (newRetryCount >= properties.getMaxRetries()) {
            markDeadLetter(record, errorMessage);
            deadLetterCounter.increment();
            log.error(
                    "Evento movido a DEAD_LETTER tras {} reintentos: id={}, aggregateId={}, eventType={}, lastError={}",
                    properties.getMaxRetries(),
                    record.id(),
                    record.aggregateId(),
                    record.eventType(),
                    errorMessage);
        } else {
            markFailed(record, newRetryCount, errorMessage);
            failedCounter.increment();
            log.warn(
                    "Fallo al procesar evento (intento {}/{}): id={}, aggregateId={}, error={}",
                    newRetryCount,
                    properties.getMaxRetries(),
                    record.id(),
                    record.aggregateId(),
                    errorMessage);
        }
    }

    private void markPublished(OutboxRecord record) {
        String sql =
                """
            UPDATE domain_event_outbox
            SET status = 'PUBLISHED', published_at = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, OffsetDateTime.now(), record.id());
    }

    private void markFailed(OutboxRecord record, int newRetryCount, String errorMessage) {
        String sql =
                """
            UPDATE domain_event_outbox
            SET status = 'FAILED', retry_count = ?, last_error = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, newRetryCount, errorMessage, record.id());
    }

    private void markDeadLetter(OutboxRecord record, String errorMessage) {
        String sql =
                """
            UPDATE domain_event_outbox
            SET status = 'DEAD_LETTER', retry_count = ?, last_error = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, properties.getMaxRetries(), errorMessage, record.id());
    }

    /**
     * Cuenta eventos pendientes para el gauge.
     */
    public int countPending() {
        String sql = "SELECT COUNT(*) FROM domain_event_outbox WHERE status = 'PENDING'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private String extractMetadataValue(String metadataJson, String key) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
            Object value = metadata.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String truncateError(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
