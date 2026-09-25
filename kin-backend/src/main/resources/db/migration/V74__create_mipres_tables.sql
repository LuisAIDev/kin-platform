-- V74: Create MIPRES tables for MinSalud integration (Resolución 740/2024)
-- MIPRES No PBSUPC - Prescripciones y Suministros no financiados con UPC

-- Tabla de prescripciones MIPRES
CREATE TABLE mipres_prescriptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    contract_id UUID,
    patient_id UUID,
    prescription_number VARCHAR(20) NOT NULL UNIQUE,
    nit VARCHAR(20) NOT NULL,
    prescription_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'AUTHORIZED', 'REJECTED', 'EXPIRED', 'CONSUMED')),
    cups_code VARCHAR(20),
    diagnosis_cie10 VARCHAR(10),
    qty_approved INT,
    unit_price_cop NUMERIC(15,2),
    raw_response JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_mipres_prescriptions_org ON mipres_prescriptions (organization_id);
CREATE INDEX idx_mipres_prescriptions_number ON mipres_prescriptions (prescription_number);
CREATE INDEX idx_mipres_prescriptions_patient ON mipres_prescriptions (patient_id);
CREATE INDEX idx_mipres_prescriptions_created ON mipres_prescriptions (created_at);
CREATE INDEX idx_mipres_prescriptions_status ON mipres_prescriptions (status);

COMMENT ON TABLE mipres_prescriptions IS 'Prescripciones MIPRES No PBSUPC (Resolución 740/2024)';
COMMENT ON COLUMN mipres_prescriptions.prescription_number IS 'Número de prescripción MIPRES (20 caracteres)';
COMMENT ON COLUMN mipres_prescriptions.nit IS 'NIT de la IPS/EPS (sin dígito de verificación)';
COMMENT ON COLUMN mipres_prescriptions.status IS 'PENDING, AUTHORIZED, REJECTED, EXPIRED, CONSUMED';

-- Tabla de suministros reportados a MIPRES
CREATE TABLE mipres_supplies (
    id UUID PRIMARY KEY,
    prescription_id UUID REFERENCES mipres_prescriptions(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    supply_id VARCHAR(50) UNIQUE, -- ID retornado por MinSalud
    prescription_number VARCHAR(20) NOT NULL,
    supply_date DATE NOT NULL,
    cups_code VARCHAR(20),
    quantity INT,
    unit_value_cop NUMERIC(15,2),
    total_value_cop NUMERIC(15,2),
    batch_number VARCHAR(50),
    expiration_date DATE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REPORTED', 'ANULLED', 'PENDING', 'REJECTED')),
    raw_request JSONB,
    raw_response JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_mipres_supplies_org ON mipres_supplies (organization_id);
CREATE INDEX idx_mipres_supplies_prescription ON mipres_supplies (prescription_id);
CREATE INDEX idx_mipres_supplies_supply_id ON mipres_supplies (supply_id);
CREATE INDEX idx_mipres_supplies_date ON mipres_supplies (supply_date);
CREATE INDEX idx_mipres_supplies_status ON mipres_supplies (status);

COMMENT ON TABLE mipres_supplies IS 'Suministros reportados a MIPRES (Resolución 740/2024)';
COMMENT ON COLUMN mipres_supplies.supply_id IS 'ID de suministro retornado por MinSalud';
COMMENT ON COLUMN mipres_supplies.status IS 'REPORTED, ANULLED, PENDING, REJECTED';

-- Tabla de cache de tokens MIPRES (opcional, para persistencia entre reinicios)
CREATE TABLE mipres_tokens (
    nit VARCHAR(20) PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE mipres_tokens IS 'Cache persistente de tokens MIPRES (token dura 24h, renovar a las 23h)';

-- Índices adicionales para consultas frecuentes
CREATE INDEX idx_mipres_prescriptions_org_status ON mipres_prescriptions (organization_id, status);
CREATE INDEX idx_mipres_supplies_org_date ON mipres_supplies (organization_id, supply_date);