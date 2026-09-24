-- ============================================================
-- V64: Miembros del equipo institucional (Sprint 2 - Fase 2.1)
-- ============================================================

CREATE TABLE IF NOT EXISTS organization_members (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id           UUID REFERENCES institutional_branches(id) ON DELETE SET NULL,
    role                VARCHAR(20) NOT NULL
                        CHECK (role IN ('IPS_ADMIN','IPS_MEDICO','IPS_ENFERMERA','IPS_FACTURADOR','IPS_AUDITOR')),
    status              VARCHAR(20) NOT NULL DEFAULT 'INVITED'
                        CHECK (status IN ('INVITED','ACTIVE','REMOVED')),
    invited_at          TIMESTAMPTZ,
    joined_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_org_member UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_org ON organization_members (organization_id);
CREATE INDEX idx_org_members_branch ON organization_members (branch_id);
CREATE INDEX idx_org_members_user ON organization_members (user_id);
