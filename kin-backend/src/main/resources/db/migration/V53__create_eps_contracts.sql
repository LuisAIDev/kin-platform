-- ============================================================
-- V53: Contratos EPS (KIN Billing - ADR-043)
-- ============================================================

CREATE TABLE IF NOT EXISTS eps_contracts (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    eps_nit                 VARCHAR(20) NOT NULL,
    eps_name                VARCHAR(200) NOT NULL,
    regimen                 VARCHAR(20) NOT NULL CHECK (regimen IN ('CONTRIBUTIVO','SUBSIDIADO','ESPECIAL','EXCEPCION')),
    contract_number         VARCHAR(100),
    start_date              DATE NOT NULL,
    end_date                DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    billing_cycle           VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' CHECK (billing_cycle IN ('WEEKLY','BIWEEKLY','MONTHLY')),
    payment_terms_days      INTEGER NOT NULL DEFAULT 60 CHECK (payment_terms_days >= 0),
    contact_email           VARCHAR(255),
    contact_phone           VARCHAR(50),
    dian_resolution_number  VARCHAR(50),
    dian_prefix             VARCHAR(10),
    dian_current_sequence   BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_eps_contract_org_eps UNIQUE (organization_id, eps_nit)
);

CREATE INDEX idx_eps_contracts_org ON eps_contracts (organization_id);
CREATE INDEX idx_eps_contracts_status ON eps_contracts (status);