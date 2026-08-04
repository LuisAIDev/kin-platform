-- ============================================================
-- V9: add current_plan_id to users
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
--
-- Cierra la brecha de esquema introducida por el commit
-- "feat: implement pricing plans and subscription system with limits" (44edbf4):
-- la entidad User ganó la relación @ManyToOne a PricingPlan (current_plan_id)
-- pero NO se creó la migración correspondiente. En dev (H2, ddl-auto: update)
-- la columna se auto-crea; en PostgreSQL (Flyway, ddl-auto: none) faltaba →
-- error "column u1_0.current_plan_id does not exist" en el login.
--
-- NULLABLE: el dominio resuelve el plan por defecto vía
-- SubscriptionValidatorService.getDefaultPlan() cuando el usuario no tiene plan.
-- ON DELETE SET NULL: borrar un PricingPlan no bloquea ni elimina usuarios.
-- Idempotente (misma convención que V5/V6).
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS current_plan_id UUID;

-- FK idempotente (misma convención que V6).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_current_plan'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_current_plan
            FOREIGN KEY (current_plan_id) REFERENCES pricing_plans (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_current_plan_id ON users (current_plan_id);
