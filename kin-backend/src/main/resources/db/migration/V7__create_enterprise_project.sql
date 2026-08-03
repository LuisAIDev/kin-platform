-- ============================================================
-- V7: enterprise_project / enterprise_document
-- (EnterpriseProjectRepositoryAdapter — persistencia del módulo Enterprise, M2G)
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
-- ============================================================

CREATE TABLE IF NOT EXISTS enterprise_project (
    project_id           UUID NOT NULL,
    version              INTEGER NOT NULL,
    status               VARCHAR(32) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    completed_at         TIMESTAMPTZ,
    failed_reason        TEXT,

    -- EnterpriseScore (@Embedded opcional)
    score_market         DOUBLE PRECISION,
    score_innovation     DOUBLE PRECISION,
    score_viability      DOUBLE PRECISION,
    score_financial      DOUBLE PRECISION,
    score_risk           DOUBLE PRECISION,
    score_scalability    DOUBLE PRECISION,
    score_team           DOUBLE PRECISION,
    score_sustainability DOUBLE PRECISION,
    score_overall        INTEGER,
    score_confidence     DOUBLE PRECISION,
    score_grade          VARCHAR(16),

    PRIMARY KEY (project_id, version),

    CONSTRAINT fk_enterprise_project_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS enterprise_document (
    id             UUID PRIMARY KEY,
    project_id     UUID NOT NULL,
    version        INTEGER NOT NULL,
    type           VARCHAR(32) NOT NULL,
    content        TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    generated_by   VARCHAR(128) NOT NULL,
    engine_version VARCHAR(32) NOT NULL,
    input_hash     VARCHAR(128) NOT NULL,
    metadata_json  TEXT,
    checksum       VARCHAR(128),
    size           BIGINT NOT NULL,
    mime_type      VARCHAR(128),
    render_format  VARCHAR(32),

    CONSTRAINT fk_enterprise_document_project
        FOREIGN KEY (project_id, version)
        REFERENCES enterprise_project (project_id, version)
        ON DELETE CASCADE,

    CONSTRAINT uq_enterprise_document_type
        UNIQUE (project_id, version, type)
);

CREATE INDEX IF NOT EXISTS idx_enterprise_document_project
    ON enterprise_document (project_id, version);
