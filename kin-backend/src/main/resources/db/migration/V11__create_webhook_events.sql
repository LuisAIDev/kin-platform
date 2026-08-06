-- ============================================================
-- V11: create webhook_events (idempotencia de webhooks de pago)
-- PostgreSQL (produccion). Dev usa ddl-auto: update.
--
-- Stripe reentrega eventos que no reciben respuesta 2xx en tiempo, o en
-- reintentos manuales. Sin registro de eventos procesados, un mismo
-- checkout.session.completed podria activar dos suscripciones. La columna
-- event_id es UNIQUE: insertar un duplicado lanza DataIntegrityViolation
-- dentro de la transaccion y se revierte sin efectos.
-- ============================================================

CREATE TABLE IF NOT EXISTS webhook_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(64) NOT NULL UNIQUE,
    event_type   VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_event_id ON webhook_events (event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_processed_at ON webhook_events (processed_at);
