-- ============================================================
-- V41: Añadir maxStorageMb y trialDays a pricing_plans
-- Permite límites de almacenamiento por plan y trial de 14 días para Personal+
-- ============================================================

-- 1. Añadir columna maxStorageMb (MB, null = ilimitado)
ALTER TABLE pricing_plans
    ADD COLUMN IF NOT EXISTS max_storage_mb INTEGER;

-- 2. Añadir columna trialDays (días de prueba, default 0)
ALTER TABLE pricing_plans
    ADD COLUMN IF NOT EXISTS trial_days INTEGER NOT NULL DEFAULT 0;

-- 3. Actualizar seeds: SALUD_PERSONAL.FREE -> 50 MB, 0 días trial
UPDATE pricing_plans
SET max_storage_mb = 50,
    trial_days = 0
WHERE code = 'FREE' AND vertical = 'SALUD_PERSONAL';

-- 4. Actualizar seeds: SALUD_PERSONAL.PERSONAL_PLUS -> 5000 MB (5 GB), 14 días trial
UPDATE pricing_plans
SET max_storage_mb = 5000,
    trial_days = 14
WHERE code = 'PERSONAL_PLUS' AND vertical = 'SALUD_PERSONAL';

-- 5. Verificar que los planes EMPRESAS tengan valores por defecto (ilimitado = NULL, trial = 0)
UPDATE pricing_plans
SET max_storage_mb = NULL,
    trial_days = 0
WHERE vertical = 'EMPRESAS' AND (max_storage_mb IS NULL OR trial_days IS NULL);

-- 6. Verificar planes SALUD_PROFESIONAL
UPDATE pricing_plans
SET max_storage_mb = NULL,
    trial_days = 0
WHERE vertical = 'SALUD_PROFESIONAL' AND (max_storage_mb IS NULL OR trial_days IS NULL);