-- ============================================================
-- V54: Tarifarios CUPS por contrato (KIN Billing - ADR-043)
-- ============================================================

CREATE TABLE IF NOT EXISTS tariffs_cups (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    contract_id         UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    cups_code           VARCHAR(20) NOT NULL,
    cups_version        VARCHAR(10) NOT NULL DEFAULT '2024',
    description         VARCHAR(500),
    unit_price_cop      NUMERIC(14,2) NOT NULL CHECK (unit_price_cop >= 0),
    max_quantity        INTEGER CHECK (max_quantity IS NULL OR max_quantity > 0),
    requires_auth       BOOLEAN NOT NULL DEFAULT FALSE,
    auth_validity_days  INTEGER CHECK (auth_validity_days IS NULL OR auth_validity_days > 0),
    cups_category       VARCHAR(50) CHECK (cups_category IN ('CONSULTA','PROCEDIMIENTO','MEDICAMENTO','EXAMEN','INSUMO','DISPOSITIVO','OTRO')),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tariff_contract_cups_date UNIQUE (contract_id, cups_code, effective_from),
    CONSTRAINT chk_tariff_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_tariffs_contract_cups ON tariffs_cups (contract_id, cups_code);
CREATE INDEX idx_tariffs_contract_effective ON tariffs_cups (contract_id, effective_from, effective_to);
CREATE INDEX idx_tariffs_category ON tariffs_cups (cups_category);