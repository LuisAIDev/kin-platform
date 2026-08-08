-- ============================================================
-- V12: create project_reports (persistencia del ConsultingReport)
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
--
-- Guarda una fila por cada generación del informe de viabilidad de un
-- proyecto (ConsultingReport). El reporte se serializa como JSON en
-- report_json; version es incremental por proyecto (1..n) para preservar
-- el histórico completo. Determinista: report_id/report_version derivan del
-- propio ConsultingReport.
--
-- project_id ON DELETE CASCADE: eliminar un proyecto elimina sus reportes.
-- ============================================================

CREATE TABLE IF NOT EXISTS project_reports (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id     UUID NOT NULL,
    version        INTEGER NOT NULL,
    report_id      UUID NOT NULL,
    report_version VARCHAR(32) NOT NULL,
    generated_at   TIMESTAMPTZ NOT NULL,
    report_json    JSONB NOT NULL,

    CONSTRAINT uk_project_reports_project_version UNIQUE (project_id, version),

    CONSTRAINT fk_project_reports_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_reports_project ON project_reports (project_id);
