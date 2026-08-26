-- ============================================================
-- V27: health telemedicine (Telemedicina, ADR-032)
-- Mensajería asíncrona paciente ↔ médico y gestión de citas.
-- PostgreSQL (todos los entornos; Flyway V1..V27).
-- ============================================================

CREATE TABLE IF NOT EXISTS messages (
    id             UUID PRIMARY KEY,
    sender_id      UUID NOT NULL,
    receiver_id    UUID NOT NULL,
    conversation_id UUID NOT NULL,
    content        TEXT NOT NULL,
    is_read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_messages_sender   FOREIGN KEY (sender_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appointments (
    id             UUID PRIMARY KEY,
    patient_id     UUID NOT NULL,
    physician_id   UUID NOT NULL,
    scheduled_at   TIMESTAMPTZ NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_appointments_patient    FOREIGN KEY (patient_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_physician  FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_appointments_status CHECK (status IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA'))
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_unread ON messages (receiver_id, is_read);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments (patient_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appointments_physician ON appointments (physician_id, scheduled_at);
