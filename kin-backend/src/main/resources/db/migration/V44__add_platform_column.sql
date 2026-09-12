-- ============================================================
-- V44: agregar columna platform a la tabla users
-- Permite aislamiento lógico entre KIN Empresas y KIN Medical.
-- PostgreSQL (todos los entornos; Flyway V1..V44).
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS platform VARCHAR(20) DEFAULT 'EMPRESAS';

-- Actualizar usuarios existentes según su rol actual
UPDATE users SET platform = 'SALUD_PERSONAL' WHERE role = 'PATIENT';
UPDATE users SET platform = 'SALUD_PROFESIONAL' WHERE role = 'PHYSICIAN';
UPDATE users SET platform = 'EMPRESAS' WHERE role IN ('FREE', 'PREMIUM', 'FACILITADOR');

-- Constraint para valores válidos
ALTER TABLE users ADD CONSTRAINT chk_users_platform
  CHECK (platform IN ('EMPRESAS', 'SALUD_PERSONAL', 'SALUD_PROFESIONAL'));

-- Índice para consultas por plataforma
CREATE INDEX IF NOT EXISTS idx_users_platform ON users (platform);
