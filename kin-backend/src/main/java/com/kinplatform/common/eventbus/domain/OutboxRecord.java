package com.kinplatform.common.eventbus.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro inmutable que representa una fila en la tabla {@code domain_event_outbox}.
 *
 * <p>Se usa tanto para lectura (relé) como para escritura (publicador).
 * El builder facilita la creación en tests y en el adaptador.</p>
 */
public record OutboxRecord(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        String metadata,
        OutboxStatus status,
        int retryCount,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt,
        String lastError
) {

    /**
     * Crea un nuevo registro en estado {@link OutboxStatus#PENDING} listo para insertar.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private UUID aggregateId;
        private String eventType;
        private String payload;
        private String metadata;
        private OutboxStatus status = OutboxStatus.PENDING;
        private int retryCount = 0;
        private OffsetDateTime createdAt = OffsetDateTime.now();
        private OffsetDateTime publishedAt;
        private String lastError;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder aggregateId(UUID aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder status(OutboxStatus status) {
            this.status = status;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder publishedAt(OffsetDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder lastError(String lastError) {
            this.lastError = lastError;
            return this;
        }

        public OutboxRecord build() {
            if (aggregateId == null) {
                throw new IllegalStateException("aggregateId es obligatorio");
            }
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalStateException("eventType es obligatorio");
            }
            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException("payload es obligatorio");
            }
            return new OutboxRecord(id, aggregateId, eventType, payload, metadata, status, retryCount, createdAt, publishedAt, lastError);
        }
    }
}
