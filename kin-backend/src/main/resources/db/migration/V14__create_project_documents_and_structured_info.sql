-- ============================================================
-- V14: project_documents / project_structured_info
-- (Información del proyecto: documentos importados + datos
--  estructurados del proyecto). PostgreSQL (todos los entornos).
-- Los documentos y la información quedan asociados al proyecto
-- (ON DELETE CASCADE). El ownership se valida en la capa de
-- servicios, igual que el resto de rutas /projects/{id}.
-- ============================================================

CREATE TABLE IF NOT EXISTS project_documents (
    id             UUID PRIMARY KEY,
    project_id     UUID NOT NULL,
    filename       VARCHAR(255) NOT NULL,
    mime_type      VARCHAR(128) NOT NULL,
    size           BIGINT NOT NULL,
    status         VARCHAR(16) NOT NULL,
    extracted_text TEXT,
    hash           VARCHAR(128),
    version        INTEGER NOT NULL DEFAULT 1,
    error_message  TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_project_documents_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_documents_project
    ON project_documents (project_id, created_at);

CREATE TABLE IF NOT EXISTS project_structured_info (
    project_id  UUID NOT NULL,
    section     VARCHAR(64) NOT NULL,
    key         VARCHAR(64) NOT NULL,
    value       TEXT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (project_id, section, key),

    CONSTRAINT fk_project_structured_info_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_structured_info_section
    ON project_structured_info (project_id, section);
