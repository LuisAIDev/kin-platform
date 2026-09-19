-- Nuevo estado: HIDDEN_FROM_PATIENT
-- Oculta el documento de la vista del paciente, pero lo conserva para médico/auditoría
ALTER TABLE clinical_documents 
    DROP CONSTRAINT IF EXISTS chk_cd_status;

ALTER TABLE clinical_documents 
    ADD CONSTRAINT chk_cd_status 
    CHECK (status IN ('ACTIVE', 'ARCHIVED', 'HIDDEN_FROM_PATIENT', 'DELETED'));

-- Índice para listar documentos visibles del paciente
CREATE INDEX IF NOT EXISTS idx_cd_patient_visible 
    ON clinical_documents (patient_id, status)
    WHERE status NOT IN ('HIDDEN_FROM_PATIENT', 'DELETED');