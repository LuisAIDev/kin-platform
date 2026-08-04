-- ============================================================
-- V10: create user_subscriptions
-- PostgreSQL (producción). Dev usa ddl-auto: update, no ejecuta migraciones.
--
-- Misma brecha de esquema que V9 (commit 44edbf4): la entidad UserSubscription
-- y sus repositorios (UserSubscriptionRepository) se añadieron SIN migración.
-- En dev (H2) la tabla se auto-crea; en PostgreSQL (Flyway) faltaba → las
-- operaciones de suscripción y el SubscriptionAccessFilter (límites por plan)
-- fallaban con "relation user_subscriptions does not exist".
--
-- user_id ON DELETE CASCADE: eliminar un usuario elimina sus suscripciones.
-- plan_id ON DELETE RESTRICT: un PricingPlan referenciado no se puede borrar
-- (integridad de facturación; el dominio desactiva planes con is_active=false).
-- CHECK de status idempotente (misma convención que V2).
-- ============================================================

CREATE TABLE IF NOT EXISTS user_subscriptions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    plan_id         UUID NOT NULL,
    start_date      TIMESTAMPTZ NOT NULL,
    end_date        TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    messages_used   INTEGER NOT NULL DEFAULT 0,
    last_reset_date TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_subscriptions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_subscriptions_plan
        FOREIGN KEY (plan_id)
        REFERENCES pricing_plans (id)
        ON DELETE RESTRICT
);

-- CHECK idempotente (misma convención que V2).
ALTER TABLE user_subscriptions DROP CONSTRAINT IF EXISTS chk_user_subscriptions_status;
ALTER TABLE user_subscriptions ADD CONSTRAINT chk_user_subscriptions_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'TRIAL'));

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_status ON user_subscriptions (user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_plan_id ON user_subscriptions (plan_id);
