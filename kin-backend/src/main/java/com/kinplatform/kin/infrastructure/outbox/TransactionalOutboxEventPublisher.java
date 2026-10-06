package com.kinplatform.kin.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import com.kinplatform.common.eventbus.EventSerializationException;
import com.kinplatform.common.eventbus.domain.OutboxRecord;
import com.kinplatform.common.eventbus.domain.OutboxStatus;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class TransactionalOutboxEventPublisher implements OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionalOutboxEventPublisher.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    @Autowired
    public TransactionalOutboxEventPublisher(JdbcTemplate jdbcTemplate,
                                             @Value("${kin.outbox.enabled:true}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.enabled = enabled;
    }

    @Override
    public void publish(DomainEvent event) {
        if (!enabled) {
            log.debug("Outbox deshabilitado (kin.outbox.enabled=false). Evento no persistido: {}", event.getClass().getSimpleName());
            return;
        }

        if (event == null) {
            throw new IllegalArgumentException("El evento no puede ser null");
        }
        if (event.aggregateId() == null) {
            throw new IllegalArgumentException("El evento debe tener aggregateId no null");
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("OutboxEventPublisher requiere una transacción activa. " +
                    "Asegúrate de llamar a publish() dentro de un método @Transactional.");
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Error serializando evento: aggregateId={}, eventType={}, error={}",
                    event.aggregateId(), event.getClass().getName(), e.getMessage(), e);
            throw EventSerializationException.of(event, e);
        }

        String metadata = buildMetadata(event);

        var record = OutboxRecord.builder()
                .id(UUID.randomUUID())
                .aggregateId((UUID) event.aggregateId())
                .eventType(event.getClass().getName())
                .payload(payload)
                .metadata(metadata)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(OffsetDateTime.now())
                .build();

        String sql = """
            INSERT INTO domain_event_outbox (id, aggregate_id, event_type, payload, metadata, status, retry_count, created_at)
            VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                record.id(),
                record.aggregateId(),
                record.eventType(),
                record.payload(),
                record.metadata(),
                record.status().name(),
                record.retryCount(),
                record.createdAt()
        );

        log.debug("Evento encolado en Outbox: id={}, type={}, aggregateId={}",
                record.id(), record.eventType(), record.aggregateId());
    }

    private String buildMetadata(DomainEvent event) {
        try {
            var metadata = new java.util.HashMap<String, Object>();
            metadata.put("timestamp", OffsetDateTime.now().toString());
            metadata.put("correlationId", java.util.UUID.randomUUID().toString());
            
            // Añadir userId si el evento implementa HasUserId
            if (event instanceof HasUserId hasUserId && hasUserId.userId() != null) {
                metadata.put("userId", hasUserId.userId().toString());
            }
            
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Error construyendo metadata para evento {}", event.getClass().getSimpleName(), e);
            return "{}";
        }
    }
}
