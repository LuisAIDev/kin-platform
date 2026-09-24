-- ============================================================
-- V66: permitir invitar miembros por email sin cuenta existente
-- (consistente con el estado INVITED ya presente en el codigo)
-- ============================================================

ALTER TABLE organization_members ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE organization_members ADD COLUMN IF NOT EXISTS invited_email VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_member_email
    ON organization_members (organization_id, LOWER(invited_email))
    WHERE invited_email IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_org_members_invited_email
    ON organization_members (invited_email)
    WHERE invited_email IS NOT NULL;
