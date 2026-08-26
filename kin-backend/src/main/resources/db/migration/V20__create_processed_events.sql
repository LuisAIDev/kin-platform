-- ============================================================
-- V20: create processed_events table for idempotency (PR 4)
-- ============================================================
-- Tabla para rastrear eventos ya procesados y evitar duplicados
-- cuando el relé del outbox reintenta la entrega (at-least-once).
-- ============================================================

CREATE TABLE processed_events (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id            VARCHAR(255) NOT NULL,              -- hash del evento o correlationId único
    aggregate_id        UUID NOT NULL,                      -- ID del agregado origen (Project, User, etc.)
    event_type          VARCHAR(120) NOT NULL,              -- FQCN del evento: com.kinplatform.kin.event.ReportGeneratedEvent
    processed_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- cuándo se procesó exitosamente
    
    CONSTRAINT uq_processed_events_event_id UNIQUE (event_id)
);

CREATE INDEX idx_processed_events_aggregate ON processed_events (aggregate_id);
CREATE INDEX idx_processed_events_type ON processed_events (event_type);
CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);

COMMENT ON TABLE processed_events IS 'Tabla para idempotencia de eventos: evita reprocesar eventos duplicados del outbox relé (at-least-once).';
COMMENT ON COLUMN processed_events.event_id IS 'Identificador único del evento (hash o correlationId) para detectar duplicados.';
COMMENT ON COLUMN processed_events.aggregate_id IS 'ID del agregado origen del evento.';
COMMENT ON COLUMN processed_events.event_type IS 'Tipo de evento (FQCN) para diagnóstico y filtrado.';
COMMENT ON COLUMN processed_events.processed_at IS 'Timestamp de cuando se procesó exitosamente el evento.';