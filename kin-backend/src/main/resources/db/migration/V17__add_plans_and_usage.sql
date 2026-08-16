-- ============================================================
-- V17: planes 3-tier (FREE/STANDARD/PREMIUM) + control de
-- consumo de IA + cuota de proyectos completados (Fase 1).
--
-- 1. pricing_plans: identificador estable `code` + presupuesto de
--    IA por período `ai_budget_usd`.
-- 2. users: contador persistente de proyectos COMPLETADOS por
--    período (eliminar un proyecto NO devuelve cupo).
-- 3. ai_usage: consumo de IA por usuario y período (tokens, costo
--    estimado, reserva atómica para gate pre-DeepSeek).
-- ============================================================

-- ------------------------------------------------------------
-- Pricing plans: code + presupuesto IA
-- ------------------------------------------------------------
ALTER TABLE pricing_plans
    ADD COLUMN code VARCHAR(20),
    ADD COLUMN ai_budget_usd NUMERIC(10, 2);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pricing_plans_code
    ON pricing_plans (code);

-- Compatibilidad: bases históricas pueden carecer de los DEFAULT de
-- billing_period/currency (creadas por un init.sql previo a Flyway). Se
-- garantiza el default 'monthly'/'USD' (misma convención de V1 y init.sql)
-- para que cualquier INSERT (migración o Hibernate, que no mapea estas
-- columnas) nunca introduzca NULL en columnas NOT NULL. Idempotente.
ALTER TABLE pricing_plans
    ALTER COLUMN billing_period SET DEFAULT 'monthly';
ALTER TABLE pricing_plans
    ALTER COLUMN currency SET DEFAULT 'USD';

-- Backfill de planes existentes (preserva FKs por id; el nombre es
-- solo cosmético y el frontend ya consume la lista por precio).
UPDATE pricing_plans
SET code = 'FREE', ai_budget_usd = 0.50
WHERE name = 'Básico Gratis' AND code IS NULL;

UPDATE pricing_plans
SET code = 'PREMIUM', ai_budget_usd = 8.75, max_projects = NULL
WHERE name = 'Premium Pro' AND code IS NULL;

-- Alta del plan STANDARD si no existe (idempotente por code). Se incluyen
-- explícitamente currency/billing_period ('USD'/'monthly') para no depender
-- del DEFAULT en bases históricas.
INSERT INTO pricing_plans (
    id, name, description, price, currency, billing_period, features,
    max_projects, messages_per_month, advanced_ai, pdf_export,
    support_level, viability_scoring_detail, is_active, code,
    ai_budget_usd, created_at, updated_at
)
SELECT gen_random_uuid(),
       'STANDARD',
       'Plan ideal para emprendedores en crecimiento: 5 proyectos completados por período y mayor cuota de IA.',
       25.00,
       'USD',
       'monthly',
       '["5 proyectos completados por periodo","IA avanzada","Scoring detallado","Exportacion a PDF","Soporte prioritario"]'::json,
       5,
       500,
       TRUE,
       TRUE,
       'PREMIUM',
       'DETAILED',
       TRUE,
       'STANDARD',
       6.25,
       now(),
       now()
WHERE NOT EXISTS (SELECT 1 FROM pricing_plans WHERE code = 'STANDARD');

-- ------------------------------------------------------------
-- users: cuota persistente de proyectos completados
-- ------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN completed_projects INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN completed_projects_period_start TIMESTAMPTZ;

-- Backfill: contador = proyectos actuales con estado COMPLETED.
UPDATE users u
SET completed_projects = COALESCE((
        SELECT count(*)
        FROM projects p
        WHERE p.user_id = u.id AND p.status = 'COMPLETED'
    ), 0),
    completed_projects_period_start = date_trunc('month', now())
WHERE u.completed_projects_period_start IS NULL;

-- ------------------------------------------------------------
-- ai_usage: consumo de IA por usuario y período
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_usage (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    period_start       TIMESTAMPTZ NOT NULL,
    period_end         TIMESTAMPTZ NOT NULL,
    input_tokens       BIGINT NOT NULL DEFAULT 0,
    output_tokens      BIGINT NOT NULL DEFAULT 0,
    total_tokens       BIGINT NOT NULL DEFAULT 0,
    estimated_cost_usd NUMERIC(12, 6) NOT NULL DEFAULT 0,
    reserved_cost_usd  NUMERIC(12, 6) NOT NULL DEFAULT 0,
    request_count      INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_ai_usage_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_ai_usage_user_period UNIQUE (user_id, period_start)
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_user
    ON ai_usage (user_id);
