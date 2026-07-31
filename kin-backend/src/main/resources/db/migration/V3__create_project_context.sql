-- ============================================================
-- V3: project_context (ContextRepository — estado durable del ProjectContext)
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
-- ============================================================

CREATE TABLE IF NOT EXISTS project_context (
    project_id   UUID PRIMARY KEY,
    context_data TEXT NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_project_context_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_context_updated_at
    ON project_context (updated_at);
