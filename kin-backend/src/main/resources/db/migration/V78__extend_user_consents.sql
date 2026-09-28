-- V78: Extend user_consents table with document_hash, revocation_reason, updated_at
ALTER TABLE user_consents
    ADD COLUMN IF NOT EXISTS document_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS revocation_reason TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Update existing rows to have updated_at = created_at
UPDATE user_consents SET updated_at = created_at WHERE updated_at IS NULL;

-- Make updated_at not null for future rows
ALTER TABLE user_consents ALTER COLUMN updated_at SET NOT NULL;

-- Add index for updated_at
CREATE INDEX IF NOT EXISTS idx_user_consents_updated_at ON user_consents(updated_at);