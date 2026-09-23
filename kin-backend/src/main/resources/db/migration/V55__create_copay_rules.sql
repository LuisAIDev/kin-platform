-- ============================================================
-- V55: Reglas de copago/cuota moderadora (KIN Billing - ADR-043)
-- ============================================================

CREATE TABLE IF NOT EXISTS copay_rules (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    contract_id             UUID NOT NULL REFERENCES eps_contracts(id) ON DELETE CASCADE,
    cups_code               VARCHAR(20),
    cups_category           VARCHAR(50) CHECK (cups_category IN ('CONSULTA','PROCEDIMIENTO','MEDICAMENTO','EXAMEN','INSUMO','DISPOSITIVO','OTRO')),
    patient_regimen         VARCHAR(20) CHECK (patient_regimen IN ('CONTRIBUTIVO','SUBSIDIADO','ESPECIAL','EXCEPCION')),
    patient_age_min         INTEGER CHECK (patient_age_min IS NULL OR patient_age_min >= 0),
    patient_age_max         INTEGER CHECK (patient_age_max IS NULL OR patient_age_max >= 0),
    copay_type              VARCHAR(20) NOT NULL CHECK (copay_type IN ('FIJO','PORCENTAJE','EXENTO','CUOTA_MODERADORA')),
    copay_value_cop         NUMERIC(14,2) CHECK (copay_value_cop IS NULL OR copay_value_cop >= 0),
    copay_cap_cop           NUMERIC(14,2) CHECK (copay_cap_cop IS NULL OR copay_cap_cop >= 0),
    priority                INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_copay_age CHECK (patient_age_max IS NULL OR patient_age_min IS NULL OR patient_age_max >= patient_age_min),
    CONSTRAINT chk_copay_value_required CHECK (
        (copay_type IN ('FIJO','PORCENTAJE','CUOTA_MODERADORA') AND copay_value_cop IS NOT NULL) OR
        (copay_type = 'EXENTO' AND copay_value_cop IS NULL)
    )
);

CREATE INDEX idx_copay_rules_contract ON copay_rules (contract_id);
CREATE INDEX idx_copay_rules_priority ON copay_rules (contract_id, priority DESC);