-- ============================================================
-- V31: health followup (Seguimiento de pacientes — ADR-033)
-- Planes de seguimiento del médico, tareas y evolución del paciente.
-- PostgreSQL (todos los entornos; Flyway V1..V31).
-- ============================================================

CREATE TABLE IF NOT EXISTS follow_up_plans (
    id           UUID PRIMARY KEY,
    physician_id UUID NOT NULL,
    patient_id   UUID NOT NULL,
    title        VARCHAR(160) NOT NULL,
    description  TEXT,
    start_date   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_date     TIMESTAMPTZ,
    frequency    VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fup_physician FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_fup_patient   FOREIGN KEY (patient_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_fup_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_fup_status    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED'))
);

CREATE TABLE IF NOT EXISTS follow_up_tasks (
    id            UUID PRIMARY KEY,
    plan_id       UUID NOT NULL,
    description   VARCHAR(300) NOT NULL,
    due_date      TIMESTAMPTZ NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at  TIMESTAMPTZ,
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fut_plan FOREIGN KEY (plan_id) REFERENCES follow_up_plans (id) ON DELETE CASCADE,
    CONSTRAINT chk_fut_status CHECK (status IN ('PENDING', 'COMPLETED', 'OVERDUE'))
);

CREATE TABLE IF NOT EXISTS patient_evolutions (
    id                  UUID PRIMARY KEY,
    patient_id          UUID NOT NULL,
    physician_id        UUID NOT NULL,
    recorded_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    symptoms            TEXT,
    vitals              JSONB,
    medication_adherence BOOLEAN,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pe_patient   FOREIGN KEY (patient_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pe_physician FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_fup_physician_patient ON follow_up_plans (physician_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_fup_patient_status ON follow_up_plans (patient_id, status);
CREATE INDEX IF NOT EXISTS idx_fut_plan ON follow_up_tasks (plan_id);
CREATE INDEX IF NOT EXISTS idx_fut_due ON follow_up_tasks (status, due_date);
CREATE INDEX IF NOT EXISTS idx_pe_patient ON patient_evolutions (patient_id, recorded_at);
