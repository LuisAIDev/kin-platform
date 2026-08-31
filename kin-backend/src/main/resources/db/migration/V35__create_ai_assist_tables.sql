-- ============================================================
-- V35: asistencia IA (ADR-037)
-- Registro de solicitudes de IA asistencial con guardrails.
-- PostgreSQL (todos los entornos; Flyway V1..V35).
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_assist_requests (
    id            UUID PRIMARY KEY,
    type          VARCHAR(20) NOT NULL,
    input_data    TEXT NOT NULL,
    response      TEXT,
    timestamp     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id       UUID NOT NULL,
    patient_id    UUID NOT NULL,
    context       VARCHAR(255),
    CONSTRAINT fk_aiassist_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_aiassist_patient FOREIGN KEY (patient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_aiassist_type CHECK (type IN ('SUMMARY', 'ORGANIZE', 'PREPARE', 'EXPLAIN', 'DRAFT'))
);

CREATE INDEX IF NOT EXISTS idx_aiassist_patient ON ai_assist_requests (patient_id);
CREATE INDEX IF NOT EXISTS idx_aiassist_user ON ai_assist_requests (user_id);
CREATE INDEX IF NOT EXISTS idx_aiassist_patient_type ON ai_assist_requests (patient_id, type);
