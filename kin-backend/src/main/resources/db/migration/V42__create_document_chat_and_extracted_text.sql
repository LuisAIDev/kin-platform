-- ============================================================
-- V42: Centro de Documentos Clínicos del paciente (ADR-041)
-- 1) clinical_documents.extracted_text: texto plano extraído del
--    archivo para el análisis conversacional con IA (caché; nunca
--    se expone al cliente vía API).
-- 2) document_chat_messages: conversación de IA por documento.
--    Escopada a document_id (FK con CASCADE): al eliminar un
--    documento se elimina su conversación.
-- PostgreSQL (todos los entornos; Flyway).
-- ============================================================

ALTER TABLE clinical_documents
    ADD COLUMN IF NOT EXISTS extracted_text TEXT;

CREATE TABLE IF NOT EXISTS document_chat_messages (
    id          UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    user_id     UUID NOT NULL,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_dcm_document FOREIGN KEY (document_id) REFERENCES clinical_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_dcm_user     FOREIGN KEY (user_id)     REFERENCES users (id)           ON DELETE CASCADE,
    CONSTRAINT chk_dcm_role    CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX IF NOT EXISTS idx_dcm_document_created ON document_chat_messages (document_id, created_at);
