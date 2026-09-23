-- ============================================================
-- V60: Cuentas por cobrar / Cartera (KIN Billing - Semana 3)
-- ============================================================
-- NOTA: aging_bucket y days_overdue NO son columnas generadas porque
-- Postgres exige expresiones INMUTABLES en GENERATED y CURRENT_DATE es
-- STABLE. Se calculan en la app (AgingCalculator) y se persisten.
-- pending_value_cop y provision_value_cop si son generadas (inmutables).

CREATE TABLE IF NOT EXISTS accounts_receivable (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL,  -- Sin FK, multi-tenancy a nivel app
    contract_id             UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    fev_invoice_id          UUID REFERENCES fev_rips_invoices(id) ON DELETE SET NULL,
    invoice_date            DATE NOT NULL,
    due_date                DATE NOT NULL,
    total_value_cop         NUMERIC(14,2) NOT NULL DEFAULT 0,
    paid_value_cop          NUMERIC(14,2) NOT NULL DEFAULT 0,
    pending_value_cop       NUMERIC(14,2) GENERATED ALWAYS AS (total_value_cop - paid_value_cop) STORED,
    days_overdue            INTEGER NOT NULL DEFAULT 0,
    aging_bucket            VARCHAR(20) NOT NULL DEFAULT 'CURRENT'
                            CHECK (aging_bucket IN ('CURRENT','1-30','31-60','61-90','91-180','180+')),
    provision_rate          NUMERIC(5,4) NOT NULL DEFAULT 0,
    provision_value_cop     NUMERIC(14,2) GENERATED ALWAYS AS ((total_value_cop - paid_value_cop) * provision_rate) STORED,
    last_payment_date       DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','PARTIAL','PAID','OVERDUE','WRITTEN_OFF')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ar_org_status ON accounts_receivable (organization_id, status);
CREATE INDEX idx_ar_org_bucket ON accounts_receivable (organization_id, aging_bucket);
CREATE INDEX idx_ar_contract ON accounts_receivable (contract_id);
CREATE INDEX idx_ar_due_date ON accounts_receivable (due_date);
