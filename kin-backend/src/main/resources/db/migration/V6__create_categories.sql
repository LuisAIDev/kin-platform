-- ============================================================
-- V6: categories (catálogo SaaS-ready que reemplaza el enum ProjectCategory)
-- PostgreSQL (producción). Dev usa ddl-auto: update + CategoryDataInitializer.
-- ============================================================

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    display_order INTEGER NOT NULL,
    icon VARCHAR(40),
    color VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed inicial (17 categorías) con UUIDs fijos deterministas.
-- ON CONFLICT (id) DO NOTHING: la migración es idempotente incluso sobre una
-- base parcialmente preparada (restore/recuperación): si la fila ya existe se
-- omite en lugar de fallar por la restricción UNIQUE/PK.
INSERT INTO categories (id, code, name, description, display_order, icon, color, active) VALUES
    ('11111111-1111-1111-1111-111111111101', 'TECNOLOGIA',       'Tecnología e Innovación', NULL,  1, NULL, '#6366f1', TRUE),
    ('11111111-1111-1111-1111-111111111102', 'EMPRESARIAL',      'Empresarial',             NULL,  2, NULL, '#0ea5e9', TRUE),
    ('11111111-1111-1111-1111-111111111103', 'AGROINDUSTRIA',    'Agroindustria',           NULL,  3, NULL, '#84cc16', TRUE),
    ('11111111-1111-1111-1111-111111111104', 'SALUD',            'Salud',                   NULL,  4, NULL, '#ef4444', TRUE),
    ('11111111-1111-1111-1111-111111111105', 'EDUCACION',        'Educación',               NULL,  5, NULL, '#f59e0b', TRUE),
    ('11111111-1111-1111-1111-111111111106', 'IMPACTO_SOCIAL',   'Impacto Social',          NULL,  6, NULL, '#f43f5e', TRUE),
    ('11111111-1111-1111-1111-111111111107', 'MEDIO_AMBIENTE',   'Medio Ambiente',          NULL,  7, NULL, '#22c55e', TRUE),
    ('11111111-1111-1111-1111-111111111108', 'INDUSTRIA',        'Industria',               NULL,  8, NULL, '#64748b', TRUE),
    ('11111111-1111-1111-1111-111111111109', 'GOBIERNO',         'Gobierno',                NULL,  9, NULL, '#8b5cf6', TRUE),
    ('11111111-1111-1111-1111-11111111110a', 'FINTECH',          'Fintech',                 NULL, 10, NULL, '#06b6d4', TRUE),
    ('11111111-1111-1111-1111-11111111110b', 'COMERCIO',         'Comercio',                NULL, 11, NULL, '#f97316', TRUE),
    ('11111111-1111-1111-1111-11111111110c', 'TURISMO',          'Turismo',                 NULL, 12, NULL, '#14b8a6', TRUE),
    ('11111111-1111-1111-1111-11111111110d', 'GASTRONOMIA',      'Gastronomía',             NULL, 13, NULL, '#e11d48', TRUE),
    ('11111111-1111-1111-1111-11111111110e', 'LOGISTICA',        'Logística',               NULL, 14, NULL, '#78716c', TRUE),
    ('11111111-1111-1111-1111-11111111110f', 'CREATIVIDAD',      'Creatividad',             NULL, 15, NULL, '#a855f7', TRUE),
    ('11111111-1111-1111-1111-111111111110', 'MARKETING_DIGITAL','Marketing Digital',       NULL, 16, NULL, '#ec4899', TRUE),
    ('11111111-1111-1111-1111-111111111111', 'INVESTIGACION',    'Investigación',           NULL, 17, NULL, '#3b82f6', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Project: reemplaza la columna enum por la FK al catálogo.
ALTER TABLE projects ADD COLUMN IF NOT EXISTS category_id UUID;

-- Backfill del enum legacy (soporta nombre del enum u ordinal, según cómo lo
-- haya persistido JPA: STRING → nombres, u ORDINAL → enteros 0..3). Es idempotente:
-- re-ejecutar solo re-asigna los mismos valores (UPDATE determinista).
--
-- CONDICIONAL a la existencia de la columna `category` (C3/C5 - Opción B):
-- en bases nuevas (V1 crea `projects` sin la columna legacy) es un no-op; en
-- bases legacy que aún tengan `category` conserva exactamente el backfill
-- original. No altera el comportamiento sobre bases que ya migraron.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'projects'
          AND column_name = 'category'
    ) THEN
        UPDATE projects p SET category_id = (SELECT id FROM categories WHERE code = 'EMPRESARIAL')
            WHERE p.category IN ('EMPRESARIAL', 'EMPRENDIMIENTO', '1', '2');
        UPDATE projects p SET category_id = (SELECT id FROM categories WHERE code = 'IMPACTO_SOCIAL')
            WHERE p.category IN ('SOCIAL', '3');
    END IF;
END $$;

-- FK idempotente: se agrega únicamente si aún no existe la restricción
-- (escenarios de restore/recuperación donde ya podría estar creada).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_projects_category'
    ) THEN
        ALTER TABLE projects ADD CONSTRAINT fk_projects_category
            FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL;
    END IF;
END $$;

-- Elimina la columna enum legacy (ya migrada; corrige la deuda de EnumType.ORDINAL).
ALTER TABLE projects DROP COLUMN IF EXISTS category;

CREATE INDEX IF NOT EXISTS idx_projects_category_id ON projects (category_id);
