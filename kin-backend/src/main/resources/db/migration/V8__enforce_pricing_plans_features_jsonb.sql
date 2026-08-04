-- ============================================================
-- V8: enforce pricing_plans.features as JSONB
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
--
-- Alinea el esquema con el mapeo corregido de PricingPlan.java
-- (@JdbcTypeCode(SqlTypes.JSON) sobre un String JSON): normaliza la columna
-- features como JSONB de forma idempotente. Ya era JSONB desde V1, pero esta
-- migración la garantiza también para bases que provengan de rutas históricas
-- (kin-database/init.sql o el script manual fix_pricing_plans_schema.sql).
--
-- Idempotente: ALTER ... TYPE JSONB sobre una columna ya JSONB es el cast
-- identidad (features::jsonb sobre jsonb no altera valores) y SET NOT NULL es
-- repetible. Cualquier valor textual no-JSON preexistente fallaría aquí de
-- forma explícita (señal correcta) y no en tiempo de ejecución.
-- ============================================================

ALTER TABLE pricing_plans
    ALTER COLUMN features TYPE JSONB USING features::jsonb,
    ALTER COLUMN features SET NOT NULL;
