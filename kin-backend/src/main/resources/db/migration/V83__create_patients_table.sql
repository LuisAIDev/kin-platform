-- ============================================================
-- V83: Módulo de Admisión de Pacientes (Fase 6.1)
-- Crea tabla patients para registro de pacientes según Res. 839/1999
-- PostgreSQL (todos los entornos; Flyway V1..V83).
-- ============================================================

CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(10) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    second_last_name VARCHAR(100),
    birth_date DATE NOT NULL,
    gender VARCHAR(1) NOT NULL CHECK (gender IN ('M', 'F', 'O', 'I')),
    phone VARCHAR(30),
    email VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(100),
    department VARCHAR(100),
    eps VARCHAR(200),
    regime VARCHAR(20) CHECK (regime IN ('CONTRIBUTIVO', 'SUBSIDIADO', 'VINCULADO', 'ESPECIAL')),
    rh_factor VARCHAR(5),
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(30),
    emergency_contact_relationship VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deletion_reason VARCHAR(500),
    CONSTRAINT uq_patients_document UNIQUE (document_type, document_number)
);

CREATE INDEX IF NOT EXISTS idx_patients_document ON patients(document_type, document_number);
CREATE INDEX IF NOT EXISTS idx_patients_names ON patients(last_name, second_last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_patients_birth_date ON patients(birth_date);
CREATE INDEX IF NOT EXISTS idx_patients_deleted_at ON patients(deleted_at);
CREATE INDEX IF NOT EXISTS idx_patients_eps ON patients(eps);
CREATE INDEX IF NOT EXISTS idx_patients_regime ON patients(regime);

-- Trigger para actualizar updated_at automáticamente
CREATE TRIGGER trg_patients_updated BEFORE UPDATE ON patients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();