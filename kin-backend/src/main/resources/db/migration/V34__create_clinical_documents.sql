-- ============================================================
-- V34: documentos clínicos compartidos (ADR-036)
-- Documentos subidos por el médico y visibles para el paciente.
-- PostgreSQL (todos los entornos; Flyway V1..V34).
-- ============================================================

CREATE TABLE IF NOT EXISTS clinical_documents (
    id            UUID PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    file_size     BIGINT NOT NULL,
    mime_type     VARCHAR(120),
    storage_key   VARCHAR(500) NOT NULL,
    uploaded_by   UUID NOT NULL,
    patient_id    UUID NOT NULL,
    physician_id  UUID,
    description   TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cd_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cd_patient  FOREIGN KEY (patient_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cd_physician FOREIGN KEY (physician_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_cd_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_cd_patient_status ON clinical_documents (patient_id, status);
CREATE INDEX IF NOT EXISTS idx_cd_physician ON clinical_documents (physician_id, status);
