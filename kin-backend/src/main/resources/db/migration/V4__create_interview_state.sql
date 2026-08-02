-- ============================================================
-- V4: interview_state (InterviewRepository — estado durable de la entrevista estratégica)
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
-- ============================================================

CREATE TABLE IF NOT EXISTS interview_state (
    project_id   UUID PRIMARY KEY,
    state_data   TEXT NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_interview_state_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_interview_state_updated_at
    ON interview_state (updated_at);
