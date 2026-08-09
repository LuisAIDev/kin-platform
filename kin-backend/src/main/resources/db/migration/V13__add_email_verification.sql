-- ============================================================
-- V13: email verification (obligatoria antes del acceso funcional)
-- ------------------------------------------------------------
-- 1) Añade users.email_verified (default FALSE: las cuentas NUEVAS
--    nacen sin verificar).
-- 2) Backfill auditable: las cuentas EXISTENTES (incluidas ADMIN y
--    FACILITADOR) se marcan como verificadas para no bloquear a
--    usuarios legítimos durante la transición. Decisión aprobada
--    ("Backfill en migración V13").
-- 3) Tabla email_verification_tokens: solo se almacena el HASH del
--    token (SHA-256), nunca el token en texto plano.
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Cuentas existentes quedan verificadas (transición reversible y auditable).
UPDATE users SET email_verified = TRUE;

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
CREATE INDEX idx_email_verification_tokens_expires ON email_verification_tokens (expires_at);
