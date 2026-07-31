-- ============================================================
-- Fix: Add missing columns to pricing_plans
-- Safe to run multiple times (idempotent)
-- ============================================================

-- Drop advanced_ai and pdf_export if they exist as wrong type (VARCHAR),
-- then re-add as BOOLEAN
ALTER TABLE pricing_plans DROP COLUMN IF EXISTS advanced_ai;
ALTER TABLE pricing_plans ADD COLUMN advanced_ai BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pricing_plans DROP COLUMN IF EXISTS pdf_export;
ALTER TABLE pricing_plans ADD COLUMN pdf_export BOOLEAN NOT NULL DEFAULT FALSE;

-- Add remaining missing columns (if not already present)
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS max_projects INTEGER;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS messages_per_month INTEGER;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS support_level VARCHAR(20) NOT NULL DEFAULT 'BASIC';
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================
-- Seed values for existing plans (by name)
-- ============================================================

UPDATE pricing_plans
SET
    description    = 'Plan gratuito para empezar con herramientas esenciales.',
    advanced_ai    = FALSE,
    pdf_export     = TRUE,
    max_projects   = 3,
    messages_per_month = 50,
    support_level  = 'BASIC',
    is_active      = TRUE
WHERE name = 'Básico Gratis'
  AND description IS NULL;

UPDATE pricing_plans
SET
    description    = 'Plan premium con inteligencia artificial avanzada y soporte prioritario.',
    advanced_ai    = TRUE,
    pdf_export     = TRUE,
    max_projects   = NULL,       -- ilimitados
    messages_per_month = 1000,
    support_level  = 'PREMIUM',
    is_active      = TRUE
WHERE name = 'Premium Pro'
  AND description IS NULL;
