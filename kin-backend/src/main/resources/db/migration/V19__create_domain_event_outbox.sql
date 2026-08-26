-- ============================================================
-- V19: Transactional Outbox para Domain Events (KIN 2.4)
-- ============================================================
-- Implementa el patrón Outbox para garantizar entrega at-least-once
-- de eventos de dominio (ReportGeneratedEvent, KnowledgeAcquiredEvent, etc.)
-- incluso si el listener falla o la infraestructura de mensajería está caída.
--
-- El publicador escribe en esta tabla DENTRO de la misma transacción
-- que el caso de uso. Un relé (OutboxRelay) lee pendientes y los publica
-- en el DomainEventBus (in-memory o externo).
--
-- Estados: PENDING -> PUBLISHED / FAILED -> DEAD_LETTER (tras max retries)
-- ============================================================

-- Habilitar extensión uuid-ossp para uuid_generate_v4() (requerida en PostgreSQL/Neon)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- Tabla Outbox transaccional
-- ============================================================
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id    UUID NOT NULL,                    -- ID del agregado origen (Project, User, etc.)
    event_type      VARCHAR(120) NOT NULL,            -- FQCN del evento: com.kinplatform.kin.event.ReportGeneratedEvent
    payload         JSONB NOT NULL,                   -- Serialización Jackson del DomainEvent completo
    metadata        JSONB,                            -- Metadatos opcionales: correlationId, causationId, timestamp, userId
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED, DEAD_LETTER
    retry_count     INT NOT NULL DEFAULT 0,           -- Contador de reintentos del relé
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),   -- Cuándo se encoló
    published_at    TIMESTAMPTZ,                      -- Cuándo se publicó exitosamente
    last_error      TEXT,                             -- Último error si falló

    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED','DEAD_LETTER'))
);

-- Índice para consultas eficientes del relé (solo pendientes, ordenados por antigüedad)
CREATE INDEX idx_outbox_pending ON domain_event_outbox (status, created_at)
    WHERE status = 'PENDING';

-- Índice para consultas por agregado (debugging, replay manual)
CREATE INDEX idx_outbox_aggregate ON domain_event_outbox (aggregate_id);

-- Comentarios para documentación en catálogo
COMMENT ON TABLE domain_event_outbox IS 'Tabla Outbox para patrón Transactional Outbox (KIN 2.4). Garantiza entrega at-least-once de Domain Events escribiendo en la misma transacción del caso de uso.';
COMMENT ON COLUMN domain_event_outbox.id IS 'Identificador único del registro en el outbox';
COMMENT ON COLUMN domain_event_outbox.aggregate_id IS 'ID del agregado de dominio que originó el evento (ProjectId, UserId, etc.)';
COMMENT ON COLUMN domain_event_outbox.event_type IS 'Nombre completo de la clase del evento (FQCN) para deserialización polimórfica';
COMMENT ON COLUMN domain_event_outbox.payload IS 'Serialización JSON del evento completo (Jackson con type info)';
COMMENT ON COLUMN domain_event_outbox.metadata IS 'Metadatos adicionales: correlationId, causationId, timestamp, userId, etc.';
COMMENT ON COLUMN domain_event_outbox.status IS 'Estado del mensaje: PENDING (por publicar), PUBLISHED (entregado), FAILED (error temporal), DEAD_LETTER (agotados reintentos)';
COMMENT ON COLUMN domain_event_outbox.retry_count IS 'Número de intentos de publicación fallidos';
COMMENT ON COLUMN domain_event_outbox.created_at IS 'Timestamp de encolado (inicio de transacción del caso de uso)';
COMMENT ON COLUMN domain_event_outbox.published_at IS 'Timestamp de publicación exitosa en el bus de eventos';
COMMENT ON COLUMN domain_event_outbox.last_error IS 'Último mensaje de error capturado durante publicación';