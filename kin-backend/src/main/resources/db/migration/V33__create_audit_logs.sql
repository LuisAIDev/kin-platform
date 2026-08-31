-- ============================================================
-- V33: auditoría de accesos a datos de salud (ADR-035)
-- Registro inmutable de accesos (usuario, acción, recurso,
-- paciente, timestamp, IP, user-agent y detalles).
-- PostgreSQL (todos los entornos; Flyway V1..V33).
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    action        VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id   UUID,
    patient_id    UUID,
    timestamp     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(255),
    details       JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_audit_patient FOREIGN KEY (patient_id) REFERENCES users (id)    ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_user_timestamp ON audit_logs (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_patient_timestamp ON audit_logs (patient_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs (action, resource_type);
