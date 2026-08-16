-- ============================================================
-- V16: create password_reset_tokens (recuperación de contraseña)
-- PostgreSQL (producción).
--
-- Solo se persiste el token_hash (SHA-256 del token original); el token en
-- sí jamás se almacena en la BD. Uso único y expiración de 24 horas.
-- user_id ON DELETE CASCADE: eliminar un usuario elimina sus tokens.
-- ============================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user
    ON password_reset_tokens (user_id);
