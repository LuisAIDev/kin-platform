-- ============================================================
-- V65: organization_id en users (multi-tenant cableado, Fase 2.1.5)
-- ============================================================
-- Sin FK todavia (la tabla organizations se añadira en Fase 2.2, V66).

ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id UUID;

CREATE INDEX IF NOT EXISTS idx_users_organization ON users (organization_id);
