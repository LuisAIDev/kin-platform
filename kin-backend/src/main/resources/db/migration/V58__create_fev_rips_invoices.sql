-- ============================================================
-- V58: FEV-RIPS - Factura Electronica de Venta con RIPS (KIN Billing - Semana 3)
-- Resolucion DIAN 000042/2020 (contingencia) y anexo tecnico FEV-RIPS.
-- ============================================================

CREATE TABLE IF NOT EXISTS fev_rips_invoices (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    contract_id             UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    batch_id                UUID NOT NULL REFERENCES rips_generation_batches(id) ON DELETE CASCADE,
    invoice_number          VARCHAR(40) NOT NULL,
    invoice_prefix          VARCHAR(10) NOT NULL,
    invoice_sequence        BIGINT NOT NULL,
    issue_date              DATE NOT NULL,
    due_date                DATE,
    total_value_cop         NUMERIC(14,2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','SIGNED','SENT_TO_DIAN','ACCEPTED','REJECTED','CONTINGENCY','VOIDED')),
    dian_cufe               VARCHAR(100),
    dian_qr_code            TEXT,
    signed_xml_path         VARCHAR(500),
    signed_pdf_path         VARCHAR(500),
    dian_response_xml       TEXT,
    contingency_reason      VARCHAR(500),
    contingency_deadline    TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fev_rips_org_prefix_sequence UNIQUE (organization_id, invoice_prefix, invoice_sequence)
);

CREATE INDEX idx_fev_rips_org_status ON fev_rips_invoices (organization_id, status);
CREATE INDEX idx_fev_rips_batch ON fev_rips_invoices (batch_id);
CREATE INDEX idx_fev_rips_contract ON fev_rips_invoices (contract_id);
