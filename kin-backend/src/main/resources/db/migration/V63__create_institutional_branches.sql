-- ============================================================
-- V63: Sedes de la IPS (Sprint 2 - Fase 2.1)
-- ============================================================

CREATE TABLE IF NOT EXISTS institutional_branches (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    name                VARCHAR(200) NOT NULL,
    address             VARCHAR(300),
    phone               VARCHAR(50),
    city                VARCHAR(100),
    services_enabled    JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_branch_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_institutional_branches_org ON institutional_branches (organization_id);
