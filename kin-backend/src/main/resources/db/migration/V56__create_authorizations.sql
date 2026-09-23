-- ============================================================
-- V56: Autorizaciones MIPRES / Manuales (KIN Billing - Semana 2)
-- ============================================================

CREATE TABLE IF NOT EXISTS authorizations (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    contract_id             UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    patient_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    authorization_number    VARCHAR(50) NOT NULL,
    authorization_type      VARCHAR(30) NOT NULL CHECK (authorization_type IN ('MIPRES','MANUAL','URGENCIA','PLAN_BENEFICIOS')),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','PARTIAL','REJECTED','EXPIRED','USED')),
    cups_codes              JSONB NOT NULL,                 -- [{"code": "890201", "qty_approved": 5, "qty_used": 2, "unit_price_cop": 45000}]
    diagnosis_cie10         JSONB,                          -- [{"code": "I10", "type": "PRINCIPAL"}]
    requested_date          TIMESTAMPTZ NOT NULL,
    approved_date           TIMESTAMPTZ,
    expiry_date             TIMESTAMPTZ,
    approved_value_cop      NUMERIC(14,2),
    used_value_cop          NUMERIC(14,2) DEFAULT 0,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_auth_number_contract UNIQUE (contract_id, authorization_number)
);

CREATE INDEX idx_authorizations_patient_status ON authorizations (patient_id, status);
CREATE INDEX idx_authorizations_contract_status ON authorizations (contract_id, status);
CREATE INDEX idx_authorizations_expiry ON authorizations (expiry_date) WHERE status IN ('APPROVED','PARTIAL');