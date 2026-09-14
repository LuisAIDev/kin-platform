-- Nuevo plan Personal Start: $2/mes con 20 triajes/mes
-- Intermedio entre Free (3 triajes) y Personal+ (ilimitados)
INSERT INTO pricing_plans (
    id, name, description, price, currency, billing_period, features,
    is_popular, display_order, max_triages_per_month, trial_days, max_patients,
    triage_sharing, viability_scoring_detail, support_level, advanced_ai,
    pdf_export, is_active, vertical, code, created_at, updated_at
)
SELECT 
    gen_random_uuid(),
    'Personal Start',
    'Para pacientes ocasionales',
    2.00,
    'USD',
    'monthly',
    '["Hasta 20 triajes por mes", "Historial completo", "Exportación PDF ilimitada", "Soporte por email"]'::json,
    TRUE,           -- is_popular: badge "Más popular"
    2,              -- display_order: entre Free (1) y Plus (3)
    20,             -- max_triages_per_month
    NULL,           -- trial_days: sin trial por ahora
    NULL,           -- max_patients: no aplica a paciente
    FALSE,          -- triage_sharing: solo en Plus
    'BASIC',        -- viability_scoring_detail
    'BASIC',        -- support_level
    FALSE,          -- advanced_ai: solo en Plus
    TRUE,           -- pdf_export: incluido en este plan
    TRUE,           -- is_active
    'SALUD_PERSONAL',
    'PERSONAL_START',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_plans 
    WHERE code = 'PERSONAL_START' AND vertical = 'SALUD_PERSONAL'
);

-- Actualizar display_order de los planes existentes
UPDATE pricing_plans SET display_order = 1 WHERE code = 'FREE' AND vertical = 'SALUD_PERSONAL';
UPDATE pricing_plans SET display_order = 3 WHERE code = 'PERSONAL_PLUS' AND vertical = 'SALUD_PERSONAL';

-- Quitar "is_popular" de Personal+ (ahora va en Personal Start)
UPDATE pricing_plans SET is_popular = FALSE WHERE code = 'PERSONAL_PLUS' AND vertical = 'SALUD_PERSONAL';