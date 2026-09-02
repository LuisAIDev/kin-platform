-- ============================================================
-- V38: fix seed data en pricing_plans (columna viability_scoring_detail NOT NULL)
-- Migración de reparación después del fallo de V37 por columnas NO NULL
-- ============================================================

-- Insertar planes KIN Salud con todas las columnas NOT NULL requeridas
-- Los inserts usan WHERE NOT EXISTS para ser idempotentes (safe re-run)

-- Plan Personal Free (3 triajes/mes gratis)
INSERT INTO pricing_plans (id, name, description, price, currency, billing_period, features, is_popular, display_order, max_triages_per_month, trial_days, max_patients, triage_sharing, viability_scoring_detail, support_level, advanced_ai, pdf_export, is_active, vertical, code, created_at, updated_at)
SELECT gen_random_uuid(),
       'Personal Free',
       'Plan gratuito para consultas de triaje ocasionales: 3 triajes por mes.',
       0.00,
       'USD',
       'monthly',
       '["Hasta 3 triajes por mes","Historial básico","Exportación PDF limitada"]'::json,
       FALSE,
       1,
       3,                          -- max_triages_per_month
       NULL,                       -- trial_days
       NULL,                       -- max_patients
       FALSE,                      -- triage_sharing
       'BASIC',                    -- viability_scoring_detail
       'BASIC',                    -- support_level
       FALSE,                      -- advanced_ai
       FALSE,                      -- pdf_export
       TRUE,                       -- is_active
       'SALUD_PERSONAL',           -- vertical
       'FREE',                     -- code
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM pricing_plans WHERE code = 'FREE' AND vertical = 'SALUD_PERSONAL');

-- Plan Personal+ (ilimitado + PDF + compartir)
INSERT INTO pricing_plans (id, name, description, price, currency, billing_period, features, is_popular, display_order, max_triages_per_month, trial_days, max_patients, triage_sharing, viability_scoring_detail, support_level, advanced_ai, pdf_export, is_active, vertical, code, created_at, updated_at)
SELECT gen_random_uuid(),
       'Personal+',
       'Plan ilimitado para consultas de triaje frecuentes: triajes ilimitados al mes, PDF ilimitado y compartir informes.',
       9.00,
       'USD',
       'monthly',
       '["Triajes ilimitados al mes","PDF ilimitado","Compartir informes","Asistente IA PRO"]'::json,
       FALSE,
       2,
       NULL,                       -- max_triages_per_month (ilimitado)
       NULL,                       -- trial_days
       NULL,                       -- max_patients
       TRUE,                       -- triage_sharing
       'BASIC',                    -- viability_scoring_detail
       'BASIC',                    -- support_level
       FALSE,                      -- advanced_ai
       FALSE,                      -- pdf_export
       TRUE,                       -- is_active
       'SALUD_PERSONAL',           -- vertical
       'PERSONAL_PLUS',            -- code
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM pricing_plans WHERE code = 'PERSONAL_PLUS' AND vertical = 'SALUD_PERSONAL');

-- Plan Profesional Trial (30 días trial completo)
INSERT INTO pricing_plans (id, name, description, price, currency, billing_period, features, is_popular, display_order, max_triages_per_month, trial_days, max_patients, triage_sharing, viability_scoring_detail, support_level, advanced_ai, pdf_export, is_active, vertical, code, created_at, updated_at)
SELECT gen_random_uuid(),
       'Profesional Trial',
       'Trial gratuito de 30 días con acceso completo a todas las funcionalidades médicas.',
       0.00,
       'USD',
       'monthly',
       '["Acceso completo 30 días","PDF ilimitado","Hasta 5 pacientes propios","Asistente IA PRO"]'::json,
       TRUE,
       1,
       NULL,                       -- max_triages_per_month
       30,                         -- trial_days (30 días)
       NULL,                       -- max_patients
       TRUE,                       -- triage_sharing
       'BASIC',                    -- viability_scoring_detail
       'BASIC',                    -- support_level
       FALSE,                      -- advanced_ai
       FALSE,                      -- pdf_export
       TRUE,                       -- is_active
       'SALUD_PROFESIONAL',        -- vertical
       'TRIAL',                    -- code
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM pricing_plans WHERE code = 'TRIAL' AND vertical = 'SALUD_PROFESIONAL');

-- Plan Profesional (pago mensual)
INSERT INTO pricing_plans (id, name, description, price, currency, billing_period, features, is_popular, display_order, max_triages_per_month, trial_days, max_patients, triage_sharing, viability_scoring_detail, support_level, advanced_ai, pdf_export, is_active, vertical, code, created_at, updated_at)
SELECT gen_random_uuid(),
       'Profesional',
       'Plan mensual para médicos profesionales: acceso completo, Límite de pacientes propio configurado.',
       35.00,
       'USD',
       'monthly',
       '["Acceso completo","Límite de pacientes propios","PDF ilimitado","Asistente IA PRO","Prioridad en soporte"]'::json,
       TRUE,
       2,
       NULL,                       -- max_triages_per_month
       NULL,                       -- trial_days
       100,                        -- max_patients (ejemplo: 100 pacientes propios)
       TRUE,                       -- triage_sharing
       'BASIC',                    -- viability_scoring_detail
       'BASIC',                    -- support_level
       FALSE,                      -- advanced_ai
       FALSE,                      -- pdf_export
       TRUE,                       -- is_active
       'SALUD_PROFESIONAL',        -- vertical
       'PROFESSIONAL',             -- code
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM pricing_plans WHERE code = 'PROFESSIONAL' AND vertical = 'SALUD_PROFESIONAL');