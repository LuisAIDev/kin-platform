-- ============================================================
-- V24: health dashboard (Dashboard de Salud del Paciente, ADR-030)
-- Perfil del paciente (factores de riesgo + condiciones crónicas)
-- y recordatorios base (citas/medicación).
-- PostgreSQL (todos los entornos; Flyway V1..V24).
-- ============================================================

CREATE TABLE IF NOT EXISTS patient_profiles (
    user_id     UUID PRIMARY KEY,
    profile_data JSONB NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_patient_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reminders (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    type        VARCHAR(30) NOT NULL CHECK (type IN ('CITA', 'MEDICACION', 'GENERAL')),
    title       VARCHAR(160) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reminders_user_active ON reminders (user_id, active);
CREATE INDEX IF NOT EXISTS idx_reminders_user_scheduled ON reminders (user_id, scheduled_at);
