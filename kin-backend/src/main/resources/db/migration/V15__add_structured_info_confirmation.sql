-- ============================================================
-- V15: trazabilidad de confirmación en project_structured_info
-- Confirmación explícita IMPORTED_DOCUMENT → USER_INPUT:
-- conserva el origen original, el documento fuente y la fecha.
-- Aditivo: solo agrega columnas opcionales, sin alterar datos.
-- ============================================================

ALTER TABLE project_structured_info
    ADD COLUMN IF NOT EXISTS original_source_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS source_document      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS confirmed_at         TIMESTAMPTZ;
