-- ============================================================
-- V5: complete pricing_plans schema
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
--
-- Cierra la deuda técnica R1: elimina la dependencia del script manual
-- kin-database/fix_pricing_plans_schema.sql convirtiéndolo en migración oficial.
--
-- Completamente idempotente: ADD COLUMN IF NOT EXISTS en cada columna.
-- Agrega ÚNICAMENTE las columnas faltantes (comparadas contra PricingPlan.java,
-- init.sql y V2). NO recrea columnas existentes (el script manual hacía
-- DROP + ADD de advanced_ai/pdf_export; aquí solo se añaden si no existen).
-- NO repite viability_scoring_detail (ya lo añade V2).
-- Compatible con bases que ya ejecutaron el script manual: en ellas todas las
-- columnas ya existen y estas sentencias son no-ops.
-- ============================================================

ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS description         TEXT;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS max_projects        INTEGER;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS messages_per_month  INTEGER;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS advanced_ai         BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS pdf_export          BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS support_level       VARCHAR(20) NOT NULL DEFAULT 'BASIC';
ALTER TABLE pricing_plans ADD COLUMN IF NOT EXISTS is_active           BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================
-- NOTA DE COMPATIBILIDAD — columna `features`
-- ------------------------------------------------------------
-- init.sql define `features JSONB NOT NULL`, mientras que la entidad
-- PricingPlan.java usa `@Column(columnDefinition = "TEXT")` sobre el mismo
-- campo (tipo de negocio String).
--
-- Esta migración NO corrige esa discrepancia (decisión explícita):
--   * En producción (PostgreSQL, ddl-auto: none) la columna permanece JSONB.
--   * La aplicación escribe features como un String JSON válido (lo produce
--     ObjectMapper en DataInitializer y en el CRUD), que PostgreSQL acepta
--     en una columna JSONB mediante coerción implícita del literal.
--   * Riesgo latente: un texto NO-JSON fallaría contra JSONB pero no contra
--     TEXT. Afecta a insert/update manuales, no al flujo normal.
--   * Unificar el tipo (p. ej. TEXT, o alinear la entidad a JSONB) requiere
--     decisión de diseño y queda fuera del alcance de esta migración R1.
-- ============================================================

-- ============================================================
-- NOTA DE DATOS (no se incluye backfill aquí)
-- ------------------------------------------------------------
-- El script manual también realizaba UPDATEs de seeding (description,
-- max_projects, etc.) para los planes existentes por nombre. Esa carga de
-- datos es responsabilidad de DataInitializer en arranque para instalaciones
-- nuevas; en instalaciones existentes los valores se conservan. Esta
-- migración se limita estrictamente al esquema (columnas faltantes).
-- ============================================================
