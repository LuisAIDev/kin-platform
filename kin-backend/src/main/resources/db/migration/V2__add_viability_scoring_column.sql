ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS viability_scoring_detail VARCHAR(20);

UPDATE pricing_plans SET viability_scoring_detail = 'BASIC' WHERE viability_scoring_detail IS NULL;

ALTER TABLE pricing_plans ALTER COLUMN viability_scoring_detail SET NOT NULL;

ALTER TABLE pricing_plans DROP CONSTRAINT IF EXISTS chk_viability_scoring_detail;
ALTER TABLE pricing_plans ADD CONSTRAINT chk_viability_scoring_detail CHECK (viability_scoring_detail IN ('BASIC', 'DETAILED'));
