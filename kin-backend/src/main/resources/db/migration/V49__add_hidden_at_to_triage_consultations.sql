-- Soft delete: el paciente puede ocultar consultas de su historial
-- El registro se mantiene en BD para cumplimiento de historia clínica
ALTER TABLE triage_consultations 
    ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS hidden_by UUID REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_triage_consultations_hidden
    ON triage_consultations(user_id)
    WHERE hidden_at IS NOT NULL;