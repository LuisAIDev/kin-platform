-- ============================================================
-- V59: Glosas EPS (KIN Billing - Semana 3)
-- Registro y workflow de apelacion de glosas recibidas de la EPS.
-- ============================================================

CREATE TABLE IF NOT EXISTS glosas (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    contract_id             UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    glosa_file_batch_id     UUID,
    eps_glosa_number        VARCHAR(50),
    rips_batch_id           UUID,
    rips_record_id          UUID,
    glosa_type              VARCHAR(20) NOT NULL
                            CHECK (glosa_type IN ('VALOR','CODIGO','CANTIDAD','AUTORIZACION','VIGENCIA','DUPLICADO','OTRO')),
    glosa_code              VARCHAR(20),
    glosa_description       VARCHAR(500),
    original_value_cop      NUMERIC(14,2) NOT NULL DEFAULT 0,
    glosa_value_cop         NUMERIC(14,2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
                            CHECK (status IN ('RECEIVED','ANALYZING','APPEALING','APPEALED','ACCEPTED','REJECTED','CONCILIATED','WRITTEN_OFF')),
    assigned_to             UUID,
    appeal_deadline         TIMESTAMPTZ,
    appeal_submitted_at     TIMESTAMPTZ,
    appeal_arguments        TEXT,
    resolution_date         TIMESTAMPTZ,
    resolved_value_cop      NUMERIC(14,2),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_glosas_org_status ON glosas (organization_id, status);
CREATE INDEX idx_glosas_contract ON glosas (contract_id);
CREATE INDEX idx_glosas_appeal_deadline ON glosas (appeal_deadline);
CREATE INDEX idx_glosas_rips_record ON glosas (rips_record_id);
