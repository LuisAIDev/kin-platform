-- ============================================================
-- V84: Marca de datos insuficientes en Enterprise (Fase 8.5b)
-- Añade is_partial a enterprise_project. Las filas existentes
-- quedan en FALSE (generación completa).
-- ============================================================

ALTER TABLE enterprise_project
    ADD COLUMN IF NOT EXISTS is_partial BOOLEAN NOT NULL DEFAULT FALSE;
