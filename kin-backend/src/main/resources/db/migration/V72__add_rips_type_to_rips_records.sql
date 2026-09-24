-- Add rips_type column to rips_records table
-- Required for RIPS generation per Anexo Técnico 1 Resolución 2275

-- Add column as nullable first (safe for existing data)
ALTER TABLE rips_records ADD COLUMN rips_type VARCHAR(10);

-- Update existing rows with a default value (US is a valid RipsType)
UPDATE rips_records SET rips_type = 'US' WHERE rips_type IS NULL;

-- Now make it NOT NULL
ALTER TABLE rips_records ALTER COLUMN rips_type SET NOT NULL;

-- Add index for efficient querying by type
CREATE INDEX idx_rips_records_rips_type ON rips_records(rips_type);

COMMENT ON COLUMN rips_records.rips_type IS 'Tipo de RIPS: US, AF, AC, AP, AU, AT (referencia RipsBatch.RipsType enum)';